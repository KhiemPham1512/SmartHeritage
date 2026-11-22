#!/bin/bash
# Khởi tạo / nâng cấp CSDL SmartHeritageDB trong Docker. An toàn khi chạy lại: không xóa dữ liệu đang có.
set -e
SQLCMD="/opt/mssql-tools18/bin/sqlcmd -C -S db -U sa -P ${MSSQL_SA_PASSWORD} -f 65001 -b"

exists=$($SQLCMD -h -1 -W -Q "SET NOCOUNT ON; SELECT COUNT(*) FROM sys.databases WHERE name = 'SmartHeritageDB'" | tr -d '[:space:]')

if [ "$exists" = "0" ]; then
  echo ">> Chưa có CSDL: tạo mới từ init_schema.sql (6+2 bảng, 3 index, 2 view, 1 hàm, 7 SP, dữ liệu mẫu)"
  $SQLCMD -i /database/init_schema.sql
else
  echo ">> CSDL đã có: giữ nguyên dữ liệu, chỉ áp migration"
fi

# Migration viết để chạy lại nhiều lần không lỗi, nên chạy cả khi vừa tạo mới
for f in /database/migrations/*.sql; do
  echo ">> migration $(basename "$f")"
  $SQLCMD -i "$f"
done
echo ">> CSDL sẵn sàng"

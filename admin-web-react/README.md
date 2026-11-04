# Smart Heritage Admin React – TV3 (W3 & W4)

This is an isolated React + TypeScript + Vite implementation of the TV3 artifact CMS.
The original `admin-web/` (plain HTML/CSS/JavaScript) and all Backend/Database files are kept unchanged.
This separation is intentional: the uploaded repository does not yet contain the React scaffold described
in the assignment document, so this React app is placed in `admin-web-react/` to avoid breaking existing work.

## Requirements
- Node.js 20 LTS or newer
- Backend FastAPI running at `http://127.0.0.1:8000`
- SQL Server schema and sample data initialized as described by the project

## Run
```bash
cd admin-web-react
npm install
cp .env.example .env
npm run dev
```
Open `http://localhost:5173`.

Set `VITE_API_BASE_URL` in `.env` if the backend runs elsewhere:
```env
VITE_API_BASE_URL=http://127.0.0.1:8000/api
```

## Authentication
The app reads `access_token` and `full_name` from localStorage, matching the existing legacy login page.
For a quick integration with the current login page, sign in through the existing `admin-web/login.html`,
then open the React app at `http://localhost:5173` on the same browser origin. If the login page and React
app use different origins/ports, localStorage is isolated by origin; use the same-origin deployment or add
a React login route in a later integration step.

## TV3 scope
- `src/pages/W3_Artifacts.tsx`: KPI cards, artifact list, text search, status/zone filters, edit and delete.
- `src/pages/W4_ArtifactForm.tsx`: bilingual create/edit form, validation, image/MP3 uploads and save.
- `src/api.ts`: fetch wrapper with Bearer token and media URL handling.
- Backend and database source files are not modified.

## Existing Backend contract used
- `GET /api/artifacts/stats`
- `GET /api/artifacts?lang=vi`
- `GET /api/artifacts/{id}?lang=vi`
- `POST /api/artifacts`
- `PUT /api/artifacts/{id}`
- `DELETE /api/artifacts/{id}`
- `POST /api/upload/media` (multipart field name: `file`)

## Notes / known integration constraints
1. The current Backend `ArtifactUpdate` schema does not accept changes to `artifact_code` or `site_id`.
   Therefore, the artifact code is locked while editing and the form omits those fields from PUT.
2. `ArtifactCreate.site_id` currently defaults to `1`; this form uses that existing default because the
   current artifact API does not expose a site selector in the TV3 contract.
3. Backend upload filenames currently use a timestamp with one-second precision. Two MP3 uploads are
   serialized by the form to reduce the chance of same-name overwrites.
4. This frontend does not invent sample/mock artifacts. Empty/error states are shown when the API cannot
   return real data.
5. `npm run build` should be run after `npm install` on the development machine. Dependency installation
   could not be completed in this execution environment, so a production build has not been verified here.

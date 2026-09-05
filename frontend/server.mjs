import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { dirname, extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(fileURLToPath(import.meta.url));
const port = Number(process.env.PORT || 5173);

const types = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8"
};

createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  const relativePath = url.pathname === "/" ? "index.html" : url.pathname.slice(1);
  const filePath = normalize(join(root, relativePath));
  if (!filePath.startsWith(normalize(root))) {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }
  try {
    const body = await readFile(filePath);
    response.writeHead(200, { "Content-Type": types[extname(filePath)] || "application/octet-stream" });
    response.end(body);
  } catch {
    const body = await readFile(join(root, "index.html"));
    response.writeHead(200, { "Content-Type": types[".html"] });
    response.end(body);
  }
}).listen(port, () => {
  console.log(`Frontend ready at http://localhost:${port}`);
});

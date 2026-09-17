const baseUrl = process.argv[2] ?? "http://localhost:8080";
const startCode = process.argv[3] ?? "NEED-MARGIN";
const searchQuery = process.argv[4] ?? "冷蔵";
const projectCode = process.argv[5];
const token = process.env.MCP_PAT;
if (!token) {
  throw new Error("MCP_PAT is required");
}

const endpoint = new URL("/mcp", baseUrl);
let protocolVersion;

async function post(message) {
  const headers = {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
    Accept: "application/json, text/event-stream"
  };
  if (protocolVersion) {
    headers["MCP-Protocol-Version"] = protocolVersion;
  }

  const response = await fetch(endpoint, {
    method: "POST",
    headers,
    body: JSON.stringify(message)
  });
  if (!response.ok) {
    throw new Error(`MCP POST failed: ${response.status} ${await response.text()}`);
  }
  if (response.status === 202 || response.status === 204) return null;

  const body = await response.text();
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) return JSON.parse(body);
  if (contentType.includes("text/event-stream")) {
    const messages = body.replaceAll("\r\n", "\n")
      .split("\n\n")
      .flatMap(event => event.split("\n"))
      .filter(line => line.startsWith("data:"))
      .map(line => JSON.parse(line.slice(5).trim()));
    return messages.find(item => item.id === message.id) ?? messages.at(-1) ?? null;
  }
  throw new Error(`Unexpected MCP response type: ${contentType}`);
}

const initialized = await post({
  jsonrpc: "2.0", id: 1, method: "initialize",
  params: { protocolVersion: "2025-11-25", capabilities: {}, clientInfo: { name: "smoke-mcp", version: "1" } }
});
if (initialized?.error) throw new Error(JSON.stringify(initialized.error));
protocolVersion = initialized.result.protocolVersion;
await post({ jsonrpc: "2.0", method: "notifications/initialized" });

const tools = await post({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });

const search = await post({
  jsonrpc: "2.0", id: 3, method: "tools/call",
  params: { name: "search_business_knowledge", arguments: { projectCode, query: searchQuery, limit: 10 } }
});

const relations = await post({
  jsonrpc: "2.0", id: 4, method: "tools/call",
  params: { name: "explore_business_relationships", arguments: { projectCode, code: startCode, depth: 2, limit: 10 } }
});

const projects = await post({
  jsonrpc: "2.0", id: 5, method: "tools/call",
  params: { name: "list_my_projects", arguments: {} }
});

const detail = await post({
  jsonrpc: "2.0", id: 6, method: "tools/call",
  params: { name: "get_business_knowledge", arguments: { projectCode, code: startCode } }
});

function textResult(message) {
  const text = message.result?.content?.find(item => item.type === "text")?.text;
  return text ? JSON.parse(text) : [];
}

const output = {
  protocolVersion,
  tools: tools.result.tools.map(tool => tool.name),
  projects: textResult(projects).map(project => project.code),
  detailCode: textResult(detail)?.code ?? null,
  searchCodes: textResult(search).map(item => item.code),
  relatedCodes: textResult(relations).map(item => item.toCode)
};
console.log(JSON.stringify(output, null, 2));

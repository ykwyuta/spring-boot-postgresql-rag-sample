const baseUrl = process.argv[2] ?? "http://localhost:8080";
const startCode = process.argv[3] ?? "NEED-MARGIN";
const token = process.env.MCP_PAT;
if (!token) {
  throw new Error("MCP_PAT is required");
}

const headers = { Authorization: `Bearer ${token}` };
const controller = new AbortController();
const response = await fetch(`${baseUrl}/sse`, { headers, signal: controller.signal });
if (!response.ok) {
  throw new Error(`SSE connection failed: ${response.status}`);
}

const reader = response.body.getReader();
const decoder = new TextDecoder();
let buffer = "";

async function nextEvent() {
  while (!buffer.includes("\n\n")) {
    const { value, done } = await reader.read();
    if (done) throw new Error("SSE connection ended unexpectedly");
    buffer += decoder.decode(value, { stream: true }).replaceAll("\r\n", "\n");
  }
  const separator = buffer.indexOf("\n\n");
  const raw = buffer.slice(0, separator);
  buffer = buffer.slice(separator + 2);
  return Object.fromEntries(raw.split("\n").map(line => {
    const index = line.indexOf(":");
    return [line.slice(0, index), line.slice(index + 1)];
  }));
}

const endpointEvent = await nextEvent();
if (endpointEvent.event !== "endpoint") {
  throw new Error(`Expected endpoint event, received ${endpointEvent.event}`);
}
const messageUrl = new URL(endpointEvent.data, baseUrl);

async function send(message) {
  const result = await fetch(messageUrl, {
    method: "POST",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify(message)
  });
  if (!result.ok) throw new Error(`MCP POST failed: ${result.status}`);
}

async function responseFor(id) {
  while (true) {
    const event = await nextEvent();
    if (event.event !== "message") continue;
    const message = JSON.parse(event.data);
    if (message.id === id) return message;
  }
}

await send({
  jsonrpc: "2.0", id: 1, method: "initialize",
  params: { protocolVersion: "2024-11-05", capabilities: {}, clientInfo: { name: "smoke-mcp", version: "1" } }
});
await responseFor(1);
await send({ jsonrpc: "2.0", method: "notifications/initialized" });

await send({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });
const tools = await responseFor(2);

await send({
  jsonrpc: "2.0", id: 3, method: "tools/call",
  params: { name: "search_business_knowledge", arguments: { query: "冷蔵", kind: "RULE", limit: 5 } }
});
const search = await responseFor(3);

await send({
  jsonrpc: "2.0", id: 4, method: "tools/call",
  params: { name: "explore_business_relationships", arguments: { code: startCode, depth: 2, limit: 10 } }
});
const relations = await responseFor(4);

function textResult(message) {
  const text = message.result?.content?.find(item => item.type === "text")?.text;
  return text ? JSON.parse(text) : [];
}

const output = {
  tools: tools.result.tools.map(tool => tool.name),
  searchCodes: textResult(search).map(item => item.code),
  relatedCodes: textResult(relations).map(item => item.toCode)
};
console.log(JSON.stringify(output, null, 2));
controller.abort();

import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
process.chdir(fileURLToPath(new URL('..', import.meta.url)));
const checks = [];
const gateway = 'http://localhost:9000';
async function check(name, fn) { await fn(); checks.push({name,passed:true}); }
async function json(url, options) { const r=await fetch(url,options); assert.equal(r.status,200,url); return r.json(); }
try {
  for (const [service,port,title,path] of [
    ['product-service',8080,'Product Service API','/api/product'],
    ['order-service',8081,'Order Service API','/api/order'],
    ['inventory-service',8082,'Inventory Service API','/api/inventory']
  ]) {
    await check(service+' direct docs and Swagger UI', async()=>{
      const spec=await json('http://localhost:'+port+'/api-docs');
      assert.equal(spec.info.title,title); assert.equal(spec.info.version,'v0.0.1'); assert.ok(spec.paths[path]);
      const ui=await fetch('http://localhost:'+port+'/swagger-ui.html'); assert.equal(ui.status,200); assert.match(await ui.text(),/Swagger UI/);
    });
    await check(service+' aggregate docs without JWT', async()=>{
      const spec=await json(gateway+'/aggregate/'+service+'/v3/api-docs');
      assert.equal(spec.info.title,title); assert.equal(spec.info.version,'v0.0.1'); assert.ok(spec.paths[path]);
      assert.equal(spec.servers[0].url,'/'); assert.equal(spec.components.securitySchemes.bearerAuth.scheme,'bearer');
      await fs.writeFile('results/'+service+'-openapi.json',JSON.stringify(spec,null,2));
    });
  }
  await check('Gateway Swagger UI without JWT',async()=>{const ui=await fetch(gateway+'/swagger-ui.html');assert.equal(ui.status,200);assert.match(await ui.text(),/Swagger UI/)});
  await check('Dropdown includes all three services',async()=>{
    const config=await json(gateway+'/v3/api-docs/swagger-config');
    assert.deepEqual(config.urls.map(u=>u.name).sort(),['Inventory Service','Order Service','Product Service']);
    await fs.writeFile('results/swagger-config.json',JSON.stringify(config,null,2));
  });
  await check('Product API still requires JWT',async()=>assert.equal((await fetch(gateway+'/api/product')).status,401));
  await check('Gateway CORS preflight allows Authorization',async()=>{
    const r=await fetch(gateway+'/api/product',{method:'OPTIONS',headers:{Origin:'http://localhost:3000','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'Authorization,Content-Type'}});
    assert.equal(r.status,200); assert.equal(r.headers.get('Access-Control-Allow-Origin'),'*');
  });
  const realm=JSON.parse(await fs.readFile('api-gateway/docker/keycloak/realms/spring-microservices-realm.json','utf8'));
  const client=realm.clients.find(c=>c.serviceAccountsEnabled);
  const token=await json('http://localhost:8181/realms/'+realm.realm+'/protocol/openid-connect/token',{
    method:'POST',body:new URLSearchParams({grant_type:'client_credentials',client_id:client.clientId,client_secret:client.secret})
  });
  await check('Real Keycloak JWT can call Product API through Gateway',async()=>{
    const products=await json(gateway+'/api/product',{headers:{Authorization:'Bearer '+token.access_token}});assert.ok(Array.isArray(products));
  });
  await fs.writeFile('results/swagger-live-results.json',JSON.stringify({passed:true,checks},null,2));
  console.log(JSON.stringify({passed:true,checks},null,2));
} catch (error) {
  await fs.writeFile('results/swagger-live-results.json',JSON.stringify({passed:false,checks,error:error.message},null,2));
  throw error;
}

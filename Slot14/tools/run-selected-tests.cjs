const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'..');
const newman=require(path.join(root,'.tools/newman/node_modules/newman'));
const output=path.join(root,'test-evidence');
fs.mkdirSync(output,{recursive:true});
newman.run({collection:path.join(root,'postman/Saga_KB1_KB8.postman_collection.json'),folder:['0 - Service readiness','KB1 - Happy path','KB2 - Thanh toán thất bại 402'],reporters:['cli','json'],reporter:{json:{export:path.join(output,'newman-KB1-KB2.json')}},timeoutRequest:30000},(err,summary)=>{
  if(err) throw err;
  const records=summary.run.executions.map(e=>({name:e.item.name,method:e.request.method,url:e.request.url.toString(),requestBody:e.request.body&&e.request.body.raw,status:e.response&&e.response.code,response:e.response&&e.response.stream.toString('utf8'),responseTime:e.response&&e.response.responseTime,assertions:e.assertions.map(a=>({name:a.assertion,pass:!a.error,error:a.error&&a.error.message}))}));
  const data={executedAt:new Date().toISOString(),timezone:'Asia/Saigon',tool:'Newman '+require(path.join(root,'.tools/newman/node_modules/newman/package.json')).version,stats:summary.run.stats,failures:summary.run.failures.map(f=>({source:f.source&&f.source.name,error:f.error.message})),variables:summary.collection.variables.toObject(),records};
  fs.writeFileSync(path.join(output,'KB1-KB2-results.json'),JSON.stringify(data,null,2)+'\n');
  process.exitCode=summary.run.failures.length?1:0;
});

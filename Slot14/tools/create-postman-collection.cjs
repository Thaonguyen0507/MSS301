const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'..');
const test = code => ({listen:'test',script:{type:'text/javascript',exec:['(() => {',...code.split('\n'), '})();']}});
const ok='pm.test("HTTP 200",()=>pm.response.to.have.status(200));';
const json='let data; try { data=pm.response.json(); } catch(e) { pm.test("JSON hợp lệ",()=>{throw e;}); }';
function req(name,method,url,body,script,description='') {
  const request={method,header:body?[{key:'Content-Type',value:'application/json'}]:[],url,description};
  if(body) request.body={mode:'raw',raw:JSON.stringify(body,null,2).replace(/"{{(kb\dOrderId)}}"/g,'{{$1}}'),options:{raw:{language:'json'}}};
  return {name,request,event:[test(script || ok)]};
}
const get=(name,url,script)=>req(name,'GET',url,null,script);
const before=(kb)=>[
  get(`[${kb}] 1 Snapshot products`,'{{inventoryUrl}}/api/inventory/products',`${ok}\n${json}\nif(Array.isArray(data)) for(const p of data) pm.collectionVariables.set("${kb}_stock_"+p.id,p.stock);`),
  get(`[${kb}] 2 Snapshot wallets`,'{{paymentUrl}}/api/payments/wallets',`${ok}\n${json}\nif(Array.isArray(data)) for(const w of data) pm.collectionVariables.set("${kb}_balance_"+w.customerId,w.balance);`)
];
function after(kb,ds={},db={}) {
  return [get(`[${kb}] 4 Check products`,'{{inventoryUrl}}/api/inventory/products',`${ok}\n${json}\nconst delta=${JSON.stringify(ds)};\nfor(const id of ["P1","P2"]) { const p=Array.isArray(data)&&data.find(x=>x.id===id); const b=Number(pm.collectionVariables.get("${kb}_stock_"+id)); pm.test("Stock "+id+" đúng độ chênh lệch",()=>pm.expect(p&&p.stock).to.eql(b+(delta[id]||0))); }`),
    get(`[${kb}] 5 Check wallets`,'{{paymentUrl}}/api/payments/wallets',`${ok}\n${json}\nconst delta=${JSON.stringify(db)};\nfor(const id of ["C1","C2"]) { const w=Array.isArray(data)&&data.find(x=>x.customerId===id); const b=Number(pm.collectionVariables.get("${kb}_balance_"+id)); pm.test("Balance "+id+" đúng độ chênh lệch",()=>pm.expect(w&&w.balance).to.eql(b+(delta[id]||0))); }`)];
}
const stepsHappy=['CREATE_ORDER','RESERVE_INVENTORY','PROCESS_PAYMENT','CONFIRM_ORDER'];
const stepsComp=['CREATE_ORDER','RESERVE_INVENTORY','SAGA','COMPENSATION','CANCEL_ORDER'];
const stepsFail=['CREATE_ORDER','SAGA','CANCEL_ORDER'];
function saga(kb,steps) {
  return get(`[${kb}] 8 Saga logs`,'{{orderUrl}}/api/orders/{{'+kb.toLowerCase()+'OrderId}}/saga-logs',`${ok}\n${json}\npm.test("Đúng thứ tự saga steps",()=>pm.expect(data.map(x=>x.step)).to.eql(${JSON.stringify(steps)}));\npm.test("Log cùng orderId",()=>pm.expect(data.every(x=>String(x.orderId)===String(pm.collectionVariables.get("${kb.toLowerCase()}OrderId")))).to.eql(true));\npm.test("Time ISO hợp lệ",()=>pm.expect(data.every(x=>typeof x.time==="string"&&!Number.isNaN(Date.parse(x.time)))).to.eql(true));\n${kb==='KB1'?'pm.test("Mọi step SUCCESS",()=>pm.expect(data.every(x=>x.result==="SUCCESS")).to.eql(true));':'pm.test("Có SAGA FAILED và kết thúc CANCEL_ORDER SUCCESS",()=>{pm.expect(data.find(x=>x.step==="SAGA").result).to.eql("FAILED");pm.expect(data[data.length-1].result).to.eql("SUCCESS");});'}`);
}
function scenario(kb,title,body,{ds={},db={},reason='',early=false}={}) {
  const happy=kb==='KB1';
  const place=req(`[${kb}] 3 Place order`,'POST','{{orderUrl}}/api/orders',body,`${ok}\n${json}\nif(data&&data.id) pm.collectionVariables.set("${kb.toLowerCase()}OrderId",data.id);\npm.test("Order status đúng",()=>pm.expect(data.status).to.eql("${happy?'CONFIRMED':'CANCELLED'}"));\npm.test("Saga status đúng",()=>pm.expect(data.sagaStatus).to.eql("${happy?'COMPLETED':'COMPENSATED'}"));\n${happy?'pm.test("Không có failureReason",()=>pm.expect(data.failureReason).to.be.null);':`pm.test("Failure reason đúng",()=>pm.expect(data.failureReason).to.match(/^${reason}/));`}`);
  const records=get(`[${kb}] 6 Check reservation`,'{{inventoryUrl}}/api/inventory/reservations',`${ok}\n${json}\nconst r=data.find(x=>String(x.orderId)===String(pm.collectionVariables.get("${kb.toLowerCase()}OrderId")));\npm.test("Reservation đúng",()=>${early?'pm.expect(r).to.eql(undefined)':`pm.expect(r&&r.status).to.eql("${happy?'RESERVED':'RELEASED'}")`});`);
  const payment=get(`[${kb}] 7 Check payments`,'{{paymentUrl}}/api/payments',`${ok}\n${json}\nconst p=data.find(x=>String(x.orderId)===String(pm.collectionVariables.get("${kb.toLowerCase()}OrderId")));\npm.test("Payment đúng",()=>${happy?'pm.expect(p&&p.status).to.eql("PAID")':'pm.expect(p).to.eql(undefined)'});`);
  return {name:`${kb} - ${title}`,description:'Chạy theo thứ tự snapshot → order → kiểm tra dữ liệu → saga logs. '+(kb==='KB6'?'MANUAL: stop payment sau snapshot; bật lại trước Check wallets/payments.':''),item:[...before(kb),place,...after(kb,ds,db),records,payment,saga(kb,happy?stepsHappy:early?stepsFail:stepsComp)]};
}
const items=[{name:'0 - Service readiness',item:[get('Products','{{inventoryUrl}}/api/inventory/products',ok),get('Wallets','{{paymentUrl}}/api/payments/wallets',ok),get('Orders','{{orderUrl}}/api/orders',ok)]},
  scenario('KB1','Happy path',{customerId:'C1',productId:'P1',quantity:2,amount:2000},{ds:{P1:-2},db:{C1:-2000}}),
  scenario('KB2','Thanh toán thất bại 402',{customerId:'C2',productId:'P1',quantity:1,amount:1000},{reason:'402'}),
  scenario('KB3','Hết hàng 409',{customerId:'C1',productId:'P2',quantity:100,amount:2000},{reason:'409',early:true}),
  scenario('KB4','Sản phẩm không tồn tại 404',{customerId:'C1',productId:'P9',quantity:1,amount:10},{reason:'404',early:true}),
  scenario('KB5','Ví không tồn tại 404',{customerId:'C9',productId:'P2',quantity:1,amount:20},{reason:'404'})];
items.find(x=>x.name.startsWith('KB2 ')).item.push(req('[KB2] 9 Direct payment 402','POST','{{paymentUrl}}/api/payments/pay',{orderId:'{{kb2OrderId}}',customerId:'C2',amount:1000},'pm.test("HTTP payment 402",()=>pm.response.to.have.status(402));\nconst e=pm.response.json();pm.test("Lý do số dư không đủ",()=>pm.expect(e.message).to.include("Số dư không đủ"));','Gọi trực tiếp payment để chứng minh HTTP 402, không tạo order hay saga mới. Ví không đủ nên không phát sinh ghi dữ liệu.'));
const kb7={name:'KB7 - Idempotent',description:'Cần chạy KB1 và KB2 trước, ID được dùng từ collection variables.',item:[...before('KB7')]};
for(let i=1;i<=2;i++) kb7.item.push(req(`[KB7] Release KB2 lần ${i}`,'POST','{{inventoryUrl}}/api/inventory/release/{{kb2OrderId}}',null,`${ok}\npm.test("RELEASED",()=>pm.expect(pm.response.text()).to.eql("RELEASED"));`),...after('KB7'));
kb7.item.push(req('[KB7] Reserve lại KB1','POST','{{inventoryUrl}}/api/inventory/reserve',{orderId:'{{kb1OrderId}}',productId:'P1',quantity:2},`${ok}\nconst r=pm.response.json();pm.test("Reservation cũ RESERVED",()=>{pm.expect(r.status).to.eql("RESERVED");pm.expect(String(r.orderId)).to.eql(String(pm.collectionVariables.get("kb1OrderId")));});`),...after('KB7'));
kb7.item.push(req('[KB7] Pay lại KB1','POST','{{paymentUrl}}/api/payments/pay',{orderId:'{{kb1OrderId}}',customerId:'C1',amount:2000},`${ok}\nconst p=pm.response.json();pm.test("Payment cũ PAID",()=>{pm.expect(p.status).to.eql("PAID");pm.expect(String(p.orderId)).to.eql(String(pm.collectionVariables.get("kb1OrderId")));});`),...after('KB7'));
kb7.item.push(req('[KB7] Refund KB2 chưa trả tiền','POST','{{paymentUrl}}/api/payments/refund/{{kb2OrderId}}',null,`${ok}\npm.test("REFUNDED",()=>pm.expect(pm.response.text()).to.eql("REFUNDED"));`),...after('KB7'));
items.push(kb7);
const kb6=scenario('KB6','MANUAL payment-service bị tắt',{customerId:'C1',productId:'P2',quantity:1,amount:20},{reason:'Service không phản hồi'});
const kb8={name:'KB8 - MANUAL compensation cũng lỗi',description:'PAYMENT_DELAY_MS=15000. Sau khi log RESERVE thành công, stop payment và inventory trong lúc order chờ. Bật lại trước khi kiểm tra dữ liệu. Xem TEST_PROMPT_KB1_KB8.md. Các request recovery phải chạy sau khi lưu bằng chứng.',item:[...before('KB8'),
req('[KB8] Place order rồi stop 2 service','POST','{{orderUrl}}/api/orders',{customerId:'C1',productId:'P2',quantity:1,amount:20},`${ok}\nconst o=pm.response.json();pm.collectionVariables.set("kb8OrderId",o.id);pm.test("Order PENDING",()=>pm.expect(o.status).to.eql("PENDING"));pm.test("Saga COMPENSATING",()=>pm.expect(o.sagaStatus).to.eql("COMPENSATING"));pm.test("Network failure",()=>pm.expect(o.failureReason).to.match(/^Service không phản hồi/));`),
get('[KB8] Saga log treo','{{orderUrl}}/api/orders/{{kb8OrderId}}/saga-logs',`${ok}\nconst logs=pm.response.json();pm.test("3 compensation failures",()=>pm.expect(logs.filter(x=>x.step==="COMPENSATION"&&x.result==="FAILED")).length(3));pm.test("Không cancel order",()=>pm.expect(logs.some(x=>x.step==="CANCEL_ORDER")).to.eql(false));`),...after('KB8',{P2:-1}),
req('[KB8] RECOVERY release sau khi lưu bằng chứng','POST','{{inventoryUrl}}/api/inventory/release/{{kb8OrderId}}',null,`${ok}\npm.test("RELEASED",()=>pm.expect(pm.response.text()).to.eql("RELEASED"));`),...after('KB8')]};
items.push({name:'Manual - KHÔNG chọn khi Run collection',item:[kb6,kb8]});
const collection={info:{name:'Slot14 Saga KB1 KB8',description:'Theo Saga_guide.md mục 6.3. KB6/KB8 cần điều khiển service thủ công. Chọn KB1 và KB2 cho báo cáo; HTTP 402 là của payment-service, order API trả 200.',schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},variable:[{key:'orderUrl',value:'http://localhost:8081'},{key:'inventoryUrl',value:'http://localhost:8082'},{key:'paymentUrl',value:'http://localhost:8083'}],item:items};
fs.mkdirSync(path.join(root,'postman'),{recursive:true});
fs.writeFileSync(path.join(root,'postman/Saga_KB1_KB8.postman_collection.json'),JSON.stringify(collection,null,2)+'\n');
console.log('Created Postman collection with KB1–KB8');

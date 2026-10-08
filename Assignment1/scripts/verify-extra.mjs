import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const gateway = 'http://localhost:9000';
const checks = [];
async function request(method, path, token, body) {
  const response = await fetch(gateway + path, { method, headers: { ...(token ? {Authorization:'Bearer '+token} : {}), ...(body ? {'Content-Type':'application/json'} : {}) }, ...(body ? {body:JSON.stringify(body)} : {}) });
  const text = await response.text();
  return {status:response.status, data:text ? JSON.parse(text) : null};
}
async function login(email,password) { const r=await request('POST','/api/auth/login',null,{email,password}); assert.equal(r.status,200); return r.data.accessToken; }
const mode = process.argv[2] || 'concurrency';
try {
  const customer = await login('an@gmail.com','123456');
  if (mode === 'outage') {
    const r = await request('POST','/api/bookings',customer,{items:[{showtimeId:'66f300000000000000000001',seatCode:'B2'}]});
    assert.equal(r.status,503); assert.match(r.data.message,/unavailable/i);
    checks.push({name:'BR14: Movie Service stopped',status:r.status,passed:true});
  } else {
    const admin = await login('admin@fucinema.com','@@abc123@@');
    const room = await request('POST','/api/rooms',admin,{roomName:'Concurrent '+Date.now(),roomType:'STANDARD',seatRows:2,seatsPerRow:4,roomStatus:'ACTIVE'}); assert.equal(room.status,201);
    const start = new Date(Date.now()+8*86400000);
    const pad=n=>String(n).padStart(2,'0');
    const startTime = start.getFullYear()+'-'+pad(start.getMonth()+1)+'-'+pad(start.getDate())+'T19:00:00';
    const showtime = await request('POST','/api/showtimes',admin,{movieId:'66f200000000000000000001',roomId:room.data.roomId,startTime,ticketPrice:150000}); assert.equal(showtime.status,201);
    const body={items:[{showtimeId:showtime.data.showtimeId,seatCode:'A1'}]};
    const parallel = await Promise.all([request('POST','/api/bookings',customer,body), request('POST','/api/bookings',customer,body)]);
    assert.deepEqual(parallel.map(r=>r.status).sort(),[201,409]);
    checks.push({name:'BR09: concurrent booking of the same seat',statuses:parallel.map(r=>r.status),passed:true});
    const booked = parallel.find(r=>r.status===201);
    const map = await request('GET','/api/bookings/showtimes/'+showtime.data.showtimeId+'/seats'); assert.deepEqual(map.data.bookedSeats,['A1']); assert.equal(map.data.availableSeats,7);
    checks.push({name:'Only one confirmed ticket persisted',passed:true});
    const report = await request('GET','/api/bookings/report?startDate='+new Date().toLocaleDateString('en-CA')+'&endDate='+new Date().toLocaleDateString('en-CA'),admin);
    assert.equal(report.status,200); assert.ok(report.data.revenueByMovie.length>=2);
    assert.equal(report.data.revenueByMovie.reduce((sum,m)=>sum+m.revenue,0),report.data.totalRevenue);
    for (let i=1;i<report.data.revenueByMovie.length;i++) assert.ok(report.data.revenueByMovie[i-1].revenue>=report.data.revenueByMovie[i].revenue);
    checks.push({name:'Revenue grouping and descending order across multiple movies',passed:true});
    await request('PUT','/api/bookings/'+booked.data.bookingId+'/cancel',admin);
    const again = await request('POST','/api/bookings',customer,body); assert.equal(again.status,201);
    checks.push({name:'Unique index releases cancelled seats',passed:true});
    await request('PUT','/api/bookings/'+again.data.bookingId+'/cancel',admin);
    await request('DELETE','/api/showtimes/'+showtime.data.showtimeId,admin);
  }
  await fs.writeFile('results/'+mode+'-results.json',JSON.stringify({mode,passed:true,checks},null,2));
  console.log(JSON.stringify({mode,passed:true,checks},null,2));
} catch (error) {
  await fs.writeFile('results/'+mode+'-results.json',JSON.stringify({mode,passed:false,checks,error:error.message},null,2));
  throw error;
}

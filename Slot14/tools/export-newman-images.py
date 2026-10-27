"""Render genuine Newman evidence as PNG reports, not Postman UI screenshots."""
from pathlib import Path
from datetime import datetime, timedelta, timezone
import hashlib
import json
import re
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'test-evidence' / 'KB1-KB2-results.json'
raw = SOURCE.read_bytes()
run = json.loads(raw.decode('utf-8-sig'))
records = run['records']
OUT = ROOT / 'test-evidence' / 'images'
OUT.mkdir(parents=True, exist_ok=True)
digest = hashlib.sha256(raw).hexdigest()[:16]
when = datetime.fromisoformat(run['executedAt'].replace('Z', '+00:00')).astimezone(timezone(timedelta(hours=7)))
fonts = Path('C:/Windows/Fonts')
def font(size, bold=False, mono=False):
    return ImageFont.truetype(str(fonts / ('consola.ttf' if mono else 'arialbd.ttf' if bold else 'arial.ttf')), size)
BODY, SMALL, MONO, BOLD, TITLE = font(30), font(25), font(27, mono=True), font(34, True), font(54, True)
INK, BLUE, GREEN, RED = '#142334', '#234D70', '#166534', '#B42318'
W, LEFT, RIGHT = 1800, 86, 1714

def record(name):
    return next(r for r in records if r['name'] == name)
def parsed(name):
    return json.loads(record(name)['response'])
def by(items, key, value):
    return next(x for x in items if x[key] == value)
def checks(kb):
    return [a for r in records if r['name'].startswith('['+kb+']') for a in r['assertions']]

class Page:
    def __init__(self, title, subtitle):
        self.im = Image.new('RGB', (W, 10000), 'white')
        self.d = ImageDraw.Draw(self.im)
        self.y = 62
        self.text('MSS301   |   SLOT14   |   COLLECTION POSTMAN CHẠY QUA NEWMAN', SMALL, BLUE)
        self.y += 12
        self.text(title, TITLE)
        self.text(subtitle, BODY)
        self.text(f"Lần chạy {when:%d/%m/%Y %H:%M:%S}  Asia/Saigon  •  {run['tool']}", SMALL, '#526475')
        self.y += 16
        self.d.line((LEFT, self.y, RIGHT, self.y), fill='#CBD5E1', width=2)
        self.y += 30
    def lines(self, text, ft=BODY, max_width=None):
        max_width = max_width or (RIGHT-LEFT)
        output=[]
        for original in str(text).split('\n'):
            if not original:
                output.append('');continue
            line=''
            for char in original:
                if line and self.d.textlength(line+char,font=ft)>max_width:
                    # Wrap on a word boundary when available, preserving all text.
                    cut=line.rfind(' ')
                    if cut>len(line)//2:
                        output.append(line[:cut]);line=line[cut+1:]+char
                    else:
                        output.append(line);line=char
                else:line+=char
            output.append(line)
        return output
    def text(self, text, ft=BODY, color=INK):
        for line in self.lines(text,ft):
            self.d.text((LEFT,self.y),line,font=ft,fill=color)
            self.y+=ft.size+12
    def heading(self,text):
        self.y+=16;self.text(text,BOLD,BLUE);self.y+=7
    def code(self,text):
        self.y+=4
        for line in self.lines(text,MONO):
            self.d.text((LEFT,self.y),line,font=MONO,fill=INK)
            self.y+=MONO.size+10
        self.y+=12
    def table(self, headers, rows, widths):
        for ri,row in enumerate([headers]+list(rows)):
            ft=font(27,True) if ri==0 else font(28)
            cell_lines=[self.lines(str(cell),ft,width-32) for cell,width in zip(row,widths)]
            height=max(len(ls) for ls in cell_lines)*(ft.size+10)+30
            x=LEFT
            for ci,width in enumerate(widths):
                self.d.rectangle((x,self.y,x+width,self.y+height),fill='#E6EEF4' if ri==0 else ('#F6F8FA' if ri%2==0 else 'white'),outline='#CFD8E0',width=2)
                cy=self.y+15
                for line in cell_lines[ci]:
                    self.d.text((x+16,cy),line,font=ft,fill=INK)
                    cy+=ft.size+10
                x+=width
            self.y+=height
        self.y+=20
    def assertions(self,items):
        for a in items:
            self.text(('PASS  ' if a['pass'] else 'FAIL  ')+a['name'],BODY,GREEN if a['pass'] else RED)
            if not a['pass']:self.text(a.get('error',''),SMALL,RED)
    def save(self,name):
        self.y+=22
        self.d.line((LEFT,self.y,RIGHT,self.y),fill='#CBD5E1',width=2);self.y+=18
        self.text('Ảnh báo cáo từ dữ liệu thực thi Newman; không phải giao diện Postman Desktop.',SMALL,'#526475')
        self.text('Nguồn: KB1-KB2-results.json  •  SHA256 '+digest,SMALL,'#526475')
        if self.y>=9900:raise RuntimeError('Report exceeds image buffer')
        output=OUT/name
        self.im.crop((0,0,W,self.y+45)).save(output,dpi=(300,300))
        print(output)

o1=parsed('[KB1] 3 Place order');o2=parsed('[KB2] 3 Place order')
summary=Page('Kết quả thực thi KB1 và KB2','Kiểm tra response, dữ liệu và saga log trên ba database riêng')
stats=run['stats']
summary.table(['Chỉ số','Kết quả thực tế'],[
('Request',str(stats['requests']['total'])),
('Request lỗi',str(stats['requests']['failed'])),
('Assertion',str(stats['assertions']['total'])),
('Assertion thất bại',str(stats['assertions']['failed'])),
('Order URL',record('[KB1] 3 Place order')['url']),
],[610,1018])
summary.heading('Kết quả từng kịch bản')
summary.table(['Kịch bản','Order ID','Kết quả','Order / Saga'],[
('KB1',o1.get('id'), 'PASS' if all(a['pass'] for a in checks('KB1')) else 'FAIL',str(o1.get('status'))+' / '+str(o1.get('sagaStatus'))),
('KB2',o2.get('id'), 'PASS' if all(a['pass'] for a in checks('KB2')) else 'FAIL',str(o2.get('status'))+' / '+str(o2.get('sagaStatus'))),
],[260,220,230,918])
summary.heading('Môi trường và cách đọc')
summary.text('MySQL 8.4, MongoDB 8.0 replica set và SQL Server 2022 chạy trong Docker project slot14-saga. Ba service Spring Boot 4.1.1 chạy trên host.')
summary.text('Cổng dùng trong lần chạy: order 18081, inventory 18082, payment 18083; tránh cổng 8081–8082 đang thuộc ứng dụng khác.')
summary.text('KB2: order API trả HTTP 200 sau khi hoàn tác; request trực tiếp tới payment-service trả HTTP 402. Hai status này được kiểm tra riêng.')
summary.save('00-newman-summary.png')

for kb,order in [('KB1',o1),('KB2',o2)]:
    r=record(f'[{kb}] 3 Place order')
    page=Page(f'{kb} request và response',f"Order ID {order.get('id')}  •  HTTP {r['status']}  •  {r['responseTime']} ms")
    page.heading('Request thực tế')
    page.code(r['method']+' '+r['url'])
    page.code(r['requestBody'])
    page.heading('Response thực tế')
    page.code(json.dumps(order,ensure_ascii=False,indent=2))
    page.heading('Assertion của request tạo đơn')
    page.assertions(r['assertions'])
    if kb=='KB2':
        direct=record('[KB2] 9 Direct payment 402')
        page.heading('Kiểm tra trực tiếp payment 402')
        page.code(direct['method']+' '+direct['url'])
        page.code(direct['requestBody'])
        page.text(f"HTTP {direct['status']}  •  {direct['responseTime']} ms",BOLD)
        page.code(json.dumps(json.loads(direct['response']),ensure_ascii=False,indent=2))
        page.assertions(direct['assertions'])
    page.save('01-KB1-response.png' if kb=='KB1' else '03-KB2-response-402.png')

    page=Page(f'{kb} dữ liệu và saga log',f"Order ID {order.get('id')}  •  Snapshot và dữ liệu sau xử lý")
    before_p=parsed(f'[{kb}] 1 Snapshot products');after_p=parsed(f'[{kb}] 4 Check products')
    before_w=parsed(f'[{kb}] 2 Snapshot wallets');after_w=parsed(f'[{kb}] 5 Check wallets')
    rows=[]
    for key in ['P1','P2']:
        b=by(before_p,'id',key)['stock'];a=by(after_p,'id',key)['stock'];rows.append((key+' stock',b,a,a-b))
    for key in ['C1','C2']:
        b=by(before_w,'customerId',key)['balance'];a=by(after_w,'customerId',key)['balance'];rows.append((key+' balance',b,a,a-b))
    page.table(['Đối tượng','Trước','Sau','Chênh lệch'],rows,[600,330,330,368])
    reservation=next((x for x in parsed(f'[{kb}] 6 Check reservation') if x['orderId']==order.get('id')),None)
    payment=next((x for x in parsed(f'[{kb}] 7 Check payments') if x['orderId']==order.get('id')),None)
    page.text('Reservation: '+(reservation.get('status') if reservation else 'không có')+'  |  Payment: '+(payment.get('status') if payment else 'không có record cho đơn này'),BOLD)
    saga=record(f'[{kb}] 8 Saga logs');logs=json.loads(saga['response'])
    page.heading('Saga log thực tế')
    page.code('GET '+saga['url'])
    page.table(['Step','Result','Message'],[(x['step'],x['result'],x['message']) for x in logs],[550,240,838])
    page.heading('Assertion saga log')
    page.assertions(saga['assertions'])
    page.heading('Thời gian ghi log')
    for x in logs:page.text(str(x['time'])+'  '+x['step'],SMALL)
    page.save('02-KB1-data-saga.png' if kb=='KB1' else '04-KB2-data-saga.png')

page=Page('Log service của hai kịch bản','Trích log ứng dụng UTF8 gắn orderId của lần chạy')
all_lines=[]
for kb,order in [('KB1',o1),('KB2',o2)]:
    page.heading(kb+'  •  Order ID '+str(order.get('id')))
    for service in ['inventory-service','payment-service','order-service']:
        source=ROOT/'test-evidence'/f'{service}.application.log'
        if not source.exists():source=ROOT/'test-evidence'/f'{service}.stdout.log'
        text=source.read_text(encoding='utf-8-sig',errors='replace')
        chosen=[line for line in text.splitlines() if ('order='+str(order.get('id'))) in line and re.search(r'\b(?:INFO|WARN|ERROR)\b',line) and (' : ' in line)]
        page.text(service,BOLD)
        for line in chosen:
            date=line.split(' ',1)[0]
            message=line.split(' : ',1)[1]
            clean=date+'  '+message
            page.text(clean,SMALL)
            all_lines.append(service+' | '+line)
        if not chosen:page.text('Không có dòng khớp bộ lọc; đối chiếu log đầy đủ.',SMALL)
page.save('05-service-logs.png')
(ROOT/'test-evidence'/'service-log-excerpts.txt').write_text('\n'.join(all_lines)+'\n',encoding='utf-8')
(OUT/'image-manifest.json').write_text(json.dumps({'source':SOURCE.name,'sourceSha256':hashlib.sha256(raw).hexdigest(),'executedAt':run['executedAt'],'tool':run['tool'],'images':sorted(p.name for p in OUT.glob('*.png')),'format':'Rendered report from actual Newman results, not Postman Desktop screenshots'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

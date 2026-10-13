import {Activity, ShieldCheck, Clock3} from 'lucide-react';
export default function Trace({events}) {
  return <details className="card trace"><summary><Activity size={18}/><span>Agent activity</span><small>{events.length} recorded steps</small></summary><div className="trace-list">{events.map(event=><div className="trace-row" key={event.id}>{event.type==='verify'?<ShieldCheck size={17}/>:<Clock3 size={17}/>}<div><b>{event.type.replace('_',' ')}</b><p>{event.detail}</p><small>{new Date(event.ts).toLocaleString('en-IN')}</small></div></div>)}</div></details>;
}

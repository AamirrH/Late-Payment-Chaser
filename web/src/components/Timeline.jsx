import {Check, Circle} from 'lucide-react';
import {stages} from '../format';
export default function Timeline({item}) {
  const ladder=['invoice_received','reminded','formal','notice','filing_ready'];const index=ladder.indexOf(item.stage);
  return <section className="card timeline"><h2>The way forward</h2><p className="muted">The next step follows the clock.</p><ol>{ladder.map((stage,i)=><li key={stage} className={i<=index?'reached':''}><span className="step-dot">{i<index?<Check size={12}/>:<Circle size={9}/>}</span><div><b>{stage==='invoice_received'?'Invoice confirmed':stages[stage]}</b><small>{['Review complete','1–7 days overdue','8–30 days overdue','31–60 days overdue','61+ days overdue'][i]}</small>{i===index&&<span className="current-label">{item.status==='paid'?'Case paid — escalation stopped':'Current stage'}</span>}</div></li>)}</ol><p className="muted">No email or WhatsApp message has been sent.</p></section>;
}

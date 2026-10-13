import {ArrowUpRight, Clock3, FileCheck2, ShieldAlert} from 'lucide-react';
export default function DemoChoices({onDemo,busy}) {
  return <section className="demo-section"><div className="section-heading"><h2>Take the flow for a spin</h2><span className="subtle-label">SYNTHETIC EXAMPLES</span></div><div className="demo-grid">{[
    ['overdue',FileCheck2,'An overdue invoice','₹50,000 · Eligible supplier','See calculation, follow-up and escalation.'],
    ['within-terms',Clock3,'Still within terms','No reminder needed','Watch the app correctly wait.'],
    ['ineligible',ShieldAlert,'An unregistered supplier','Eligibility comes first','See why the calculation is blocked.']
  ].map(([kind,Icon,title,subtitle,desc])=><button disabled={busy} key={kind} className="demo-card" onClick={()=>onDemo(kind)}><div className="demo-card-top"><Icon size={22}/><ArrowUpRight size={18}/></div><h3>{title}</h3><b>{subtitle}</b><p>{desc}</p></button>)}</div></section>;
}

import {Calculator} from 'lucide-react';
import {money,date} from '../format';
export default function Formula({calc}) {
  return <details className="card formula"><summary><Calculator size={18}/><span>Behind the numbers</span><small>View calculation</small></summary><div className="formula-body"><p className="formula-expression">{calc.formula}</p><dl><div><dt>Principal</dt><dd>{money(calc.principal)}</dd></div><div><dt>Bank rate (decimal)</dt><dd>{calc.bankRate} · dated {date(calc.bankRateDate)}</dd></div><div><dt>Annual rate (3 × bank rate)</dt><dd>{calc.annualRate}</dd></div><div><dt>Full calendar months</dt><dd>{calc.fullMonths}</dd></div><div><dt>Remaining days / 365</dt><dd>{calc.remainingDays} days</dd></div><div><dt>Rounding</dt><dd>Final result only · half up to paise</dd></div></dl><p className="muted">Configured estimate. The demo uses an illustrative bank rate and a convention that still needs legal review.</p></div></details>;
}

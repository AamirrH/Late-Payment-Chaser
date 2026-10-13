import {stages} from '../format';
export default function Badge({item}) {
  const state=item.status==='paid'?'paid':!item.confirmed?(item.eligibilityReason?'ineligible':'review'):item.stage;
  return <span className={`badge ${state}`}>{state==='review'?'Needs review':state==='ineligible'?'Not eligible':stages[state]}</span>;
}

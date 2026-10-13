export function money(value='0.00') {
  const [whole, fraction='00'] = String(value).split('.');
  return `₹${new Intl.NumberFormat('en-IN').format(BigInt(whole || '0'))}.${fraction.padEnd(2,'0')}`;
}
export const stages={invoice_received:'Within terms',reminded:'Friendly reminder',formal:'Formal reminder',notice:'Interest notice',filing_ready:'Filing ready',paid:'Paid'};
export function date(value) { return value ? new Date(value+'T00:00:00').toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'}) : 'Not confirmed'; }

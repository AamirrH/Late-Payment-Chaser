import {useState} from 'react';
import {LoaderCircle, X, CheckCircle2} from 'lucide-react';
import {api,signIn,download} from './api';
import Login from './components/Login';
import Shell from './components/Shell';
import Profile from './components/Profile';
import CaseList from './components/CaseList';
import Upload from './components/Upload';
import Confirm from './components/Confirm';
import CaseDetail from './components/CaseDetail';

const demoProfile={businessName:'Sample Riya Design Studio',udyamNumber:'UDYAM-ZZ-00-0000000',category:'small'};
export default function App(){
  const [session,setSession]=useState(null);const [route,setRoute]=useState('list');const [profile,setProfile]=useState(demoProfile);
  const [data,setData]=useState({cases:[]});const [item,setItem]=useState(null);const [busy,setBusy]=useState(false);const [error,setError]=useState('');const [message,setMessage]=useState('');
  async function run(fn){setBusy(true);setError('');setMessage('');try{await fn();}catch(e){setError(e.message);}finally{setBusy(false);}}
  const refresh=async()=>setData(await api('/cases'));
  const navigate=next=>run(async()=>{if(next==='list')await refresh();setRoute(next);window.scrollTo(0,0);});
  const open=id=>run(async()=>{setItem(await api('/cases/'+id));setRoute('detail');window.scrollTo(0,0);});
  const action=(path,body)=>run(async()=>setItem(await api(`/cases/${item.id}/${path}`,{method:'POST',body})));
  const demo=kind=>run(async()=>{setItem(await api('/cases/demo/'+kind,{method:'POST'}));setRoute('detail');window.scrollTo(0,0);});
  const start=()=>run(async()=>{const s=await signIn();setSession(s);const p=await api('/profile');setProfile(p.businessName?p:s.mode==='demo'?demoProfile:{businessName:'',udyamNumber:'',category:'micro'});await refresh();setRoute(p.businessName?'list':'profile');});
  const upload=file=>run(async()=>{const body=new FormData();body.append('file',file);const created=await api('/cases',{method:'POST',body});setItem(created);setRoute('detail');setItem(await api(`/cases/${created.id}/extract`,{method:'POST'}));});
  const confirm=form=>run(async()=>{const c=await api(`/cases/${item.id}/confirm`,{method:'PATCH',body:form});setItem(c);if(c.confirmed&&c.lastCalc.daysOverdue>0&&c.stage!=='invoice_received')setItem(await api(`/cases/${c.id}/draft`,{method:'POST',body:{tone:'friendly'}}));});
  return <>{error&&<div className="toast error" role="alert"><span>{error}</span><button onClick={()=>setError('')} aria-label="Dismiss error"><X size={17}/></button></div>}{message&&<div className="toast success" role="status"><CheckCircle2 size={18}/>{message}<button onClick={()=>setMessage('')} aria-label="Dismiss message"><X size={17}/></button></div>}{busy&&<div className="busy" role="status"><LoaderCircle size={17} className="spin"/> Working on your request…</div>}{!session?<Login onStart={start} busy={busy}/>:<Shell route={route} navigate={navigate} profile={profile} mode={session.mode}>
    {route==='profile'&&<Profile profile={profile} setProfile={setProfile} busy={busy} mode={session.mode} onSave={()=>run(async()=>{await api('/profile',{method:'PUT',body:profile});await refresh();setRoute('list');})}/>}
    {route==='list'&&<CaseList data={data} busy={busy} mode={session.mode} onOpen={open} onAdd={()=>navigate('upload')} onDemo={demo}/>}
    {route==='upload'&&<Upload onUpload={upload} onDemo={demo} busy={busy} mode={session.mode}/>}
    {route==='detail'&&item&&(!item.confirmed?<Confirm key={item.id} item={item} profile={profile} busy={busy} onConfirm={confirm}/>:<CaseDetail item={item} busy={busy} onBack={()=>navigate('list')} onDraft={tone=>action('draft',{tone})} onSimulate={()=>action('simulate',{days:30})} onPaid={()=>{if(window.confirm('Mark this case paid? This permanently stops escalation for this case.'))action('paid');}} onDelete={()=>{if(window.confirm('Delete this case, its invoice and draft history?'))run(async()=>{await api(`/cases/${item.id}`,{method:'DELETE'});await refresh();setRoute('list');});}} onDownload={(id,type)=>run(()=>download(`/cases/${item.id}/notices/${id}.${type}`,`notice.${type}`))} onCopy={id=>run(async()=>{const blob=await api(`/cases/${item.id}/notices/${id}.txt`,{blob:true});await navigator.clipboard.writeText(await blob.text());setMessage('Notice copied. Review it before sending.');})} onFiling={()=>run(()=>download(`/cases/${item.id}/filing-pack`,'filing-pack.pdf','POST'))}/>) }
  </Shell>}</>;
}

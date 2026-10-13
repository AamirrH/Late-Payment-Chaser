const base = import.meta.env.VITE_API_URL || '/api';
let cloudUser = null;
export async function signIn() {
  if (import.meta.env.VITE_AUTH_MODE === 'firebase') {
    const {initializeApp} = await import('firebase/app');
    const {getAuth, signInAnonymously} = await import('firebase/auth');
    const app = initializeApp({apiKey:import.meta.env.VITE_FIREBASE_API_KEY,authDomain:import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,projectId:import.meta.env.VITE_FIREBASE_PROJECT_ID,appId:import.meta.env.VITE_FIREBASE_APP_ID});
    cloudUser = (await signInAnonymously(getAuth(app))).user;
    return {mode:'cloud'};
  }
  const session = await api('/demo/session',{method:'POST'});
  sessionStorage.setItem('chaser-token',session.token);
  return session;
}
export async function api(path,options={}) {
  const token = cloudUser ? await cloudUser.getIdToken() : sessionStorage.getItem('chaser-token');
  const headers = {...(token?{Authorization:`Bearer ${token}`}:{})};
  let body=options.body;
  if(body && !(body instanceof FormData)) { headers['Content-Type']='application/json'; body=JSON.stringify(body); }
  const response=await fetch(base+path,{...options,body,headers:{...headers,...options.headers}});
  if(!response.ok) {
    const error=await response.json().catch(()=>({message:'Service unavailable. Check that the finance service is running.'}));
    throw new Error(error.message || error.detail || 'Request could not be completed.');
  }
  return options.blob ? response.blob() : response.json();
}
export async function download(path,filename,method='GET') {
  const blob=await api(path,{method,blob:true});
  const url=URL.createObjectURL(blob); const link=document.createElement('a'); link.href=url; link.download=filename; link.click(); setTimeout(()=>URL.revokeObjectURL(url),1000);
}

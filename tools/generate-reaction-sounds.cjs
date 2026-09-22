// Deterministic, original creature chirps; PCM WAV, no external audio assets.
const fs=require('fs');
const rate=22050;
for(const [name,notes] of Object.entries({hello:[440,660,880],surprise:[330,920],giggle:[660,540,740,600]})){
 const duration=0.48,n=Math.floor(rate*duration),b=Buffer.alloc(44+n*2);
 b.write('RIFF',0);b.writeUInt32LE(36+n*2,4);b.write('WAVEfmt ',8);b.writeUInt32LE(16,16);b.writeUInt16LE(1,20);b.writeUInt16LE(1,22);b.writeUInt32LE(rate,24);b.writeUInt32LE(rate*2,28);b.writeUInt16LE(2,32);b.writeUInt16LE(16,34);b.write('data',36);b.writeUInt32LE(n*2,40);
 let phase=0;
 for(let i=0;i<n;i++){
  const t=i/rate,segment=t/duration*notes.length,index=Math.min(notes.length-1,Math.floor(segment)),u=segment-index;
  const freq=notes[index]*(1+0.16*Math.sin(u*Math.PI));phase+=2*Math.PI*freq/rate;
  const envelope=Math.sin(Math.PI*u)**2 * Math.min(1,t/0.01)*Math.min(1,(duration-t)/0.035);
  const voice=(Math.sin(phase)+0.22*Math.sin(2*phase)+0.08*Math.sin(3*phase))/1.3;
  b.writeInt16LE(Math.round(voice*envelope*11000),44+i*2);
 }
 fs.writeFileSync('app/src/main/res/raw/'+name+'.wav',b);
}

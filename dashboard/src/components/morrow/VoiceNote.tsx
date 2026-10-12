/** A Theo voice note. Plays the recording when it exists; the text always carries the content. */
export function VoiceNote({ href }: { href: string }) {
  return (
    <div className="desk-voice">
      <span>Voice note</span>
      <audio controls preload="none" src={href} />
    </div>
  );
}

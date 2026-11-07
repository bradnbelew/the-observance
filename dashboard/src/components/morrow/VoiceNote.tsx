import { voiceLink } from "@/lib/morrow/desk";
import { voiceFileFor } from "@/lib/morrow/serve";

/**
 * A Theo voice note: the player only sees it once the recording exists (a missing file used to leave a
 * dead audio control that 404'd). The text always carries the content.
 */
export async function VoiceNote({ id }: { id: string }) {
  if (!/^[\w-]+$/.test(id) || !(await voiceFileFor(id))) return null;
  return (
    <div className="desk-voice">
      <span>Voice note</span>
      <audio controls preload="none" src={voiceLink(id)} />
    </div>
  );
}

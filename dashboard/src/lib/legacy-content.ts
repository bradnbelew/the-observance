export interface PublicServer {
  id: string;
  name: string;
  version: string;
  players: string;
  location: string;
  status: string;
  listingLabel?: string;
  recoveredDocket?: boolean;
}

export const publicServers: PublicServer[] = [
  { id: "112", name: "Cedar Valley SMP", version: "1.6.4", players: "0/24", location: "Chicago, IL", status: "offline" },
  { id: "386", name: "Block Party Creative", version: "1.7.2", players: "0/40", location: "Dallas, TX", status: "offline" },
  { id: "522", name: "Kingdoms of Aster", version: "1.5.2", players: "0/18", location: "Chicago, IL", status: "offline" },
  { id: "731", name: "Saturday Miners", version: "1.6.4", players: "0/12", location: "Atlanta, GA", status: "offline" },
  { id: "906", name: "Red County Tekkit", version: "Tekkit 3", players: "0/30", location: "Dallas, TX", status: "offline" },
  { id: "1174", name: "Oak & Iron", version: "1.7.10", players: "0/20", location: "Chicago, IL", status: "offline" },
  { id: "1439", name: "Little Harbor", version: "1.7.9", players: "0/16", location: "Atlanta, GA", status: "offline" },
  { id: "chi-ret-7f0a", name: "The Observance", version: "unknown", players: "0/7", location: "Chicago, IL", status: "expired", listingLabel: "Docket reference damaged", recoveredDocket: true },
  { id: "1911", name: "FamilyCraft East", version: "1.7.10", players: "0/10", location: "New York, NY", status: "offline" },
];

export const announcements = [
  { id: "panel", date: "November 3, 2014", title: "Legacy panel access", body: "The TCAdmin 1 control panel is now available in read-only mode for expired accounts. Console access, restarts and file uploads remain disabled. Customers needing a final backup should open a billing ticket before December 1." },
  { id: "billing", date: "July 20, 2014", title: "Billing system maintenance", body: "The client area will be unavailable Sunday from 2:00 AM to 5:00 AM Central while we move the billing database. Existing game servers will not be affected. Password reset messages may be delayed during the maintenance window." },
  { id: "java", date: "April 11, 2013", title: "Java 7 image update", body: "Our Minecraft images now use Java 7 by default. Customers running older Bukkit builds can select the Java 6 image from the game panel. Please make a backup before changing the startup profile." },
  { id: "chicago", date: "September 8, 2012", title: "Chicago node migration complete", body: "Game nodes chi-gs1 through chi-gs4 have been moved to the new rack. All assigned IP addresses remain unchanged. Submit a support ticket if your server does not appear in the panel." },
  { id: "billing2", date: "January 2, 2011", title: "Invoice reminder changes", body: "Past-due reminders are now sent three and one days before suspension. World data is retained for fourteen days after cancellation and then removed from active storage." },
];

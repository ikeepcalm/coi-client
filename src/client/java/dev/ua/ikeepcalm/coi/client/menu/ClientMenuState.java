package dev.ua.ikeepcalm.coi.client.menu;

/**
 * The menu document currently on screen.
 * <p>
 * Pure state: the routing (open the screen, replace it, close it) lives in
 * {@code CircleOfImaginationClient} beside the other payload receivers, and the
 * screen re-reads {@link #document()} rather than being handed one, so a push
 * that lands while it is open shows up on the next frame.
 * <p>
 * {@link #revision()} is what tells the screen a genuinely new document arrived.
 * Comparing {@code version} would not do: the server mints a fresh session (and
 * restarts its versions) for every menu it opens.
 */
public final class ClientMenuState {

    private static MenuDocument document;
    private static int revision;
    private static boolean fromSheet;

    private ClientMenuState() {
    }

    public static void adopt(MenuDocument next) {
        if (next == null || next.closed()) return;
        document = next;
        revision++;
    }

    public static MenuDocument document() {
        return document;
    }

    public static boolean hasDocument() {
        return document != null;
    }

    public static int revision() {
        return revision;
    }

    /**
     * Remembers that this menu session was reached from the character sheet, so
     * the back arrow at the root of the server's screen has somewhere to go.
     * <p>
     * Only {@code CharacterSheetScreen.openTarget} sets it: the sheet's
     * destination cards are the one doorway that leaves a page behind. Pressing
     * the menu key straight from the world does not, and there the back arrow
     * keeps meaning "close".
     */
    public static void markFromSheet() {
        fromSheet = true;
    }

    public static boolean openedFromSheet() {
        return fromSheet;
    }

    public static String session() {
        return document != null ? document.session() : "";
    }

    public static int version() {
        return document != null ? document.version() : 0;
    }

    /**
     * Forgets the document without touching the screen — the caller decides
     * what happens to that.
     */
    public static void clear() {
        document = null;
        fromSheet = false;
    }

    public static void reset() {
        clear();
        revision = 0;
    }

    /**
     * A hand-written document exercising every component type, for the F8 debug
     * screen. It goes through {@link MenuParser} rather than building records
     * directly, so the offline check covers the parser too.
     */
    public static void debugInject() {
        MenuDocument sample = MenuParser.parse(SAMPLE);
        if (sample != null) adopt(sample);
    }

    /**
     * Vocabulary v2 in one screen: a hero, disclosure rows, a step rail, chip
     * facts and two panel grids, alongside every v1 type and every extension
     * field. It is deliberately longer than any real menu — this is the only
     * harness that exists with no server attached, so a reviewer has to be able
     * to judge the whole vocabulary from it.
     */
    private static final String SAMPLE = """
            {"session":"debug","version":1,"screen":"debug.sample",
             "title":"Church of the Fool","subtitle":"Sanctum of Dreams · 12 members",
             "accent":"B347CC","icon":{"kind":"pathway","value":"fool"},
             "back":true,"closable":true,
             "toast":{"style":"warn","text":"Your bishopric expires in 3 days."},
             "sections":[
               {"components":[
                 {"type":"hero","icon":{"kind":"pathway","value":"fool"},
                  "title":"Worms of Spirit","subtitle":"The creature is sleeping",
                  "badge":"DORMANT","badgeColor":"5FD35F","style":"ring",
                  "fraction":0.42,"fractionLabel":"04:12","color":"B347CC",
                  "chips":[{"label":"Stability","value":"Complete","color":"5FD35F"},
                           {"label":"Cost","value":"20 madness"},
                           {"label":"Seq","value":"7","color":"B347CC","hint":"Your lowest held sequence."},
                           {"label":"Held","value":"14 days"}]}]},
               {"title":"Standing","components":[
                 {"type":"text","text":"You serve as a Deacon. Deacons may admit members and open the vault, but cannot declare war or appoint successors.","style":"body"},
                 {"type":"chips","items":[
                   {"label":"Rank","value":"Deacon","color":"B347CC","icon":{"kind":"pathway","value":"fool"},"hint":"Promoted 3 days ago."},
                   {"label":"Tithe","value":"120/day"},
                   {"label":"Vault","value":"Open","color":"5FD35F"},
                   {"label":"Standing","value":"Good","color":"7FC8FF","hint":"No censure on record."},
                   {"label":"Ordeals attended","value":"9"},
                   {"label":"Sponsorships","value":"2"}]},
                 {"type":"stat","label":"Devotion","value":"1,240 / 2,000","fraction":0.62,"color":"B347CC","style":"ring","delta":"+140","icon":{"kind":"glyph","value":"spirituality"}},
                 {"type":"stat","label":"Sanity","value":"64%","fraction":0.64,"cap":0.82,"style":"segments","color":"5FD35F","delta":"-3","hint":"Permanent madness caps this at 82%."},
                 {"type":"stat","label":"Tithe owed","value":"120","hint":"Paid automatically at dawn."},
                 {"type":"kv","rows":[
                   {"label":"Rank","value":"Deacon","icon":{"kind":"pathway","value":"fool"}},
                   {"label":"Joined","value":"14 days ago"},
                   {"label":"Sponsor","value":"Klein","color":"7FC8FF","hint":"The member who vouched for you.","icon":{"kind":"glyph","value":"soul"}}]},
                 {"type":"note","style":"warn","title":"Ordeal pending","text":"An ordeal is scheduled for the next crimson moon. Attendance is expected of every deacon.","icon":{"kind":"glyph","value":"cooldown"}},
                 {"type":"checklist","items":[
                   {"ok":true,"state":"ok","label":"Sequence 6 reached"},
                   {"ok":true,"state":"ok","label":"Acting complete","detail":"100% of 100%","icon":{"kind":"glyph","value":"authority"}},
                   {"ok":false,"state":"pending","label":"Ordeal attended","detail":"Next crimson moon.","color":"E0A83C"},
                   {"ok":false,"state":"no","label":"Ritual site consecrated","detail":"No site within 200 blocks."}]},
                 {"type":"divider","label":"Preferences"},
                 {"type":"toggle","id":"toggle_notify","label":"Announce my rituals","on":true,"desc":"Other members see a notice when you begin one.","icon":{"kind":"glyph","value":"spirit"}},
                 {"type":"toggle","id":"toggle_war","label":"Receive war orders","on":false,"enabled":false,"disabledReason":"Only bishops receive war orders."}]},
               {"title":"How a bid works","id":"howto","icon":{"kind":"glyph","value":"divination"},"badge":"5 STEPS","collapsed":false,"components":[
                 {"type":"steps","style":"numbered","items":[
                   {"title":"Hold the full bracket","text":"Every seat below yours, without a gap.","done":true},
                   {"title":"Bank your bid","text":"Overflow is escrowed until the seat is decided.","done":true},
                   {"title":"The weakest holder is named","text":"Measured at the moment the bid closes."},
                   {"title":"Wait out the vigil","done":false},
                   {"title":"Take the seat"}]},
                 {"type":"steps","style":"timeline","items":[
                   {"title":"Bid opened","text":"3 days ago","done":true,"icon":{"kind":"glyph","value":"authority"}},
                   {"title":"Vigil begins","text":"In 4 hours"},
                   {"title":"Seat decided"}]},
                 {"type":"details","id":"hiding_limits","summary":"What hiding does not cover","style":"muted","text":[
                   "Hiding does not make you invisible, and it does not hide the effects of what you do while hidden.",
                   "A Beyonder with stronger divination can still find you, and a Seer can name the street you are standing on."]},
                 {"type":"details","id":"escrow","summary":"Where the escrow goes","open":true,"style":"body","icon":{"kind":"glyph","value":"cost"},"text":[
                   "An overflowing bid is escrowed, not spent. It returns in full if the seat goes to somebody else.",
                   "The church takes no cut of an escrow, which is why a failed bid costs only the vigil."]}]},
               {"title":"Archive","id":"archive","badge":"3","collapsed":true,"icon":{"kind":"glyph","value":"growth"},"components":[
                 {"type":"kv","rows":[
                   {"label":"Last ordeal","value":"41 days ago"},
                   {"label":"Last war","value":"Never"},
                   {"label":"Founded","value":"Year 1349"}]}]},
               {"title":"Outcomes","components":[
                 {"type":"panels","columns":2,"cells":[
                   {"id":"win","icon":{"kind":"item","value":"minecraft:golden_apple"},"title":"Victory","value":"+1 sequence",
                    "subtitle":"Your bid is spent and the seat is yours.","color":"5FD35F","fraction":0.7,
                    "badge":"70%","tooltip":["Measured against the current holder.","Recomputed hourly."],"action":"pick_win"},
                   {"id":"lose","icon":{"kind":"item","value":"minecraft:wither_rose"},"title":"Failure","value":"-20 madness",
                    "subtitle":"The escrow returns; the vigil does not.","color":"E04C4C","fraction":0.3,"badge":"30%","action":"pick_lose"}]},
                 {"type":"divider","label":"Score factors"},
                 {"type":"panels","columns":3,"cells":[
                   {"id":"f1","title":"Rarity","value":"8.4","subtitle":"How few hold it.","color":"7FC8FF","fraction":0.84,"tooltip":["Read-only: this cell has no action."]},
                   {"id":"f2","title":"Tenure","value":"3.1","subtitle":"How long you have held it.","color":"E0A83C","fraction":0.31},
                   {"id":"f3","title":"Reach","value":"—","subtitle":"Needs a bishopric.","enabled":false,"disabledReason":"Only a bishop is measured for reach.","badge":"LOCKED","badgeColor":"E04C4C"}]}]},
               {"title":"Members","components":[
                 {"type":"list","id":"members","searchable":true,"empty":"Nobody has joined yet.",
                  "rows":[
                    {"id":"m1","title":"Klein Moretti","subtitle":"Bishop · Fool · Seq 7","icon":{"kind":"pathway","value":"fool"},"badge":"ONLINE","badgeColor":"5FD35F","meta":"12,400 dev","fraction":0.95,"color":"B347CC","action":"member_klein","tooltip":["Joined 90 days ago","Devotion 12,400"]},
                    {"id":"m2","title":"Audrey Hall","subtitle":"Deacon · Spectator · Seq 8","icon":{"kind":"pathway","value":"spectator"},"badge":"AWAY","badgeColor":"D8B44A","meta":"6,100 dev","fraction":0.48,"action":"member_audrey"},
                    {"id":"m3","title":"Alger Wilson","subtitle":"Member · Sailor · Seq 7","icon":{"kind":"pathway","value":"sailor"},"badge":"OFFLINE","action":"member_alger"},
                    {"id":"m4","title":"Leonard Mitchell","subtitle":"Member · Sun · Seq 7","icon":{"kind":"pathway","value":"sun"},"badge":"EXILED","badgeColor":"D35F5F","enabled":false,"disabledReason":"Exiled members cannot be inspected."},
                    {"id":"m5","title":"Xio Derecha","subtitle":"Member · Hunter · Seq 8","icon":{"kind":"pathway","value":"hunter"},"action":"member_xio"}]}]},
               {"title":"Relics","components":[
                 {"type":"grid","columns":4,"size":"large","cells":[
                   {"id":"g1","title":"Blincy Blade","icon":{"kind":"glyph","value":"damage"},"badge":"0-08","badgeColor":"B347CC","color":"B347CC","action":"relic_1","tooltip":["Sealed Artifact 0-08","Sequence 4 · Blade"]},
                   {"id":"g2","title":"Lucky Coin","icon":{"kind":"glyph","value":"cost"},"color":"D8B44A","action":"relic_2"},
                   {"id":"g3","title":"Grimoire","icon":{"kind":"glyph","value":"magic"},"badge":"NEW","badgeColor":"5FD35F","action":"relic_3"},
                   {"id":"g5","title":"Sealed","icon":{"kind":"glyph","value":"ward"},"enabled":false,"disabledReason":"Requires a bishop to unseal."},
                   {"id":"g6","title":"Marionette","icon":{"kind":"ability","value":"fool-marionette"},"color":"B347CC","action":"relic_4","tooltip":["The ability icon route: pack model first,","bundled category art second — never blank."]}]},
                 {"type":"grid","columns":10,"size":"small","cells":[
                   {"id":"s1","icon":{"kind":"item","value":"minecraft:emerald"},"badge":"3","color":"5FD35F","action":"stack_1"},
                   {"id":"s2","icon":{"kind":"item","value":"minecraft:redstone"},"action":"stack_2"},
                   {"id":"s3","icon":{"kind":"item","value":"minecraft:lapis_lazuli"},"action":"stack_3"},
                   {"id":"s4","icon":{"kind":"item","value":"minecraft:quartz"},"enabled":false}]},
                 {"type":"spacer","size":4},
                 {"type":"input","id":"gift","label":"Send a gift","placeholder":"player name","maxLength":16,"submit":"send_gift","submitLabel":"Send","hint":"The recipient must be a member."}]},
               {"title":"Actions","components":[
                 {"type":"buttons","columns":2,"buttons":[
                   {"id":"pray","label":"Pray","style":"primary","desc":"Once per day"},
                   {"id":"donate","label":"Donate","style":"secondary","desc":"Convert gold to devotion"},
                   {"id":"leave","label":"Leave the church","style":"danger","confirm":{"title":"Leave the church?","body":"You will lose your rank, your devotion and your vault access. Rejoining requires a sponsor.","confirmLabel":"Leave"}},
                   {"id":"bless","label":"Bless a member","style":"success","enabled":false,"disabledReason":"Only bishops may bless."}]},
                 {"type":"button","id":"vault","label":"Open the artifact vault","style":"ghost","desc":"Opens the server's own screen","icon":{"kind":"glyph","value":"uniqueness"}}]},
               {"title":"Hero, plain style","components":[
                 {"type":"hero","icon":{"kind":"item","value":"minecraft:beacon"},
                  "title":"Sanctum of Dreams","subtitle":"Consecrated 41 days ago",
                  "badge":"ACTIVE","style":"plain","fraction":0.78,"fractionLabel":"78%","color":"7FC8FF",
                  "chips":[{"label":"Radius","value":"200 blocks"},{"label":"Upkeep","value":"40/day","color":"E0A83C"}]}]}],
             "footer":[{"id":"refresh","label":"Refresh","style":"secondary"},
                       {"id":"close","label":"Done","style":"primary"}]}
            """;
}

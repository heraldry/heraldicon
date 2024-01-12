(ns heraldicon.frontend.news
  (:require
   [heraldicon.frontend.title :as title]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(defn- release-image [img-src]
  (let [src (static/static-url img-src)]
    [:a {:href src
         :target "_blank"}
     [:img {:style {:width "100%"}
            :src src
            :alt "release update overview"}]]))

(defn view []
  (rf/dispatch [::title/set :string.text.title/home])
  [:div
   {:style {:padding "10px"
            :text-align "justify"
            :min-width "30em"
            :max-width "60em"
            :margin "auto"}}
   [:div.release-row
    [:div.info
     [:h2 "News"]
     [:p "In many cases new features are rolled out incrementally, without big release. But now and then I'll group some features and new development and post an update here, so it is easy to stay informed."]]]

   [:h3 "2024-01-12 - Happy new year!"]
   [:div.release-row
    [:div.info
     [:p "I wish you all health and a splendid upcoming year."]
     [:p "There weren't as many visible changes this year, but there were several improvements in the backend and a lot of new arms and charges created by many of you:"]
     [:ul
      [:li "2,400+ users"]
      [:li "30,000+ arms, over 3,000 of them public"]
      [:li "7,000+ charges, over 3,000 of them public"]
      [:li "600+ collections, over 100 of them public"]]
     [:p "Just as last year: thanks to everybody who helped with feedback, feature requests, charge creations, especially public ones, community themes, community escutcheons, and all the other ways many of you contributed!"]]]

   [:h3 "2023-03-19 - Bordure/orle line style adjustments and flory line styles"]
   [:div.release-row
    [:div.info
     [:p "Flory and flory-counter-flory are now available line styles."]
     [:p "Introducing those new line styles made me realize that the previous offsetting and spacing of line styles for bordures and orles was a bit unpredictable and difficult. This is now greatly improved, I believe. With a reasonable default offset being chosen to align the line style with the top center of the surrounding field shape."]
     [:p
      [:span {:style {:color "darkred"}} [:b "Some of your coats of arms may look different now if they use orles or bordures with line styles."]] "."]
     [:p "Please have a look and maybe adjust ordinaries or charges if necessary, unfortunately this was another case that was difficult to migrate without breaking some existing arms."]]]

   [:h3 "2023-01-12 - Escutcheon adjustments"]
   [:div.release-row
    [:div.info
     [:p "I just went over all the existing escutcheons and improved their shapes and how they define the field and where the fess point is."]
     [:p "This changed some of the escutcheons quite a bit, and even tho the vast majority of existing coats of arms is not affected, "
      [:span {:style {:color "darkred"}} [:b "some of your coats of arms may look different now if they use these escutcheons"]] "."]
     [:p "Please have a look and maybe adjust ordinaries or charges if necessary, I apologize for the inconvenience. This kind of change should be a very rare event."]]]

   [:h3 "2023-01-05 - Happy new year!"]
   [:div.release-row
    [:div.info
     [:p "I hope everybody had a good start into the year."]
     [:p "Heraldicon recently reached a few milestones, there are now:"]
     [:ul
      [:li "over 1,200 users"]
      [:li "over 1,700 public arms, over 10,000 private arms"]
      [:li "over 1,000 public charges, over 3,200 private charges"]
      [:li "over 50 public collections, over 280 private collections"]]
     [:p "Thanks to everybody who helped with feedback, feature requests, charge creations, especially public ones, community themes, community escutcheons, and all the other ways many of you contributed!"]]]

   [:h3 "2022-09-01 - New rendering engine and various improvements"]
   [:div.release-row
    [:div.info
     [:p "There have been various new small features over the past few months and a large rewrite of the rendering system, which improves some existing conditions and will allow many planned features that weren't possible in the previous version."]
     [:p "New features and changes:"]
     [:ul
      [:li [:strong "new rendering engine"] " - this is the biggest change, albeit largely invisible, some immediate differences are:"
       [:ul
        [:li "components are much more aware of their surrounding field's environment and shape"]
        [:li "line styles now detect the exact intersection with the shape and by default use the line length as a base for their width option; a new 'Size reference' option for lines allows configuration of this"]
        [:li "exact bounding boxes for all components can now be calculated, which allows more precise resizing of the achievement, based on its helms, ornaments, etc."]
        [:li "cottises inherit the parent ordinary's properties, synching their line styles with the ordinary"]
        [:li "couped/voided ordinaries can now be fimbriated"]
        [:li [:strong "some of these changes are not fully backwards compatible"] ", so if you see changes in your arms, especially in partitions, ordinaries, cottises, line styles, or fimbriation, then that may be why; I try to migrate data, but sometimes that's not feasible... and hopefully these cases can be fixed easily"]]]
      [:li "a 'Gyronny N' field partition for an arbitrary number of subfields"]
      [:li "a mechanism to load arms into a field (in the top right of the field form), e.g. to use existing arms in subfields"]
      [:li "many new escutcheons and some new themes, mostly provided by the community on the Discord server, thanks!"]
      [:li "sort options for lists, as well as a counter of accessible and filtered items"]
      [:li "'Copy to new', exporting, and sharing are now grouped in a new action button"]
      [:li "'Copy to new' now also works with items of other users, automatically filling in the appropriate source attribution"]
      [:li "improved licensing and attribution, also fixed a bug where BY-NC-SA and BY-SA were compatible"]
      [:li "new anchor/orientation points:"
       [:ul
        [:li [:stron "center"] " - the exact middle of the field's bounding box, often coincides with 'fess' on subfields"]
        [:li [:stron "hoist"] " - a point left of the center for flags, similar to 'fess' in arms"]
        [:li [:stron "fly"] " - a point right of the center for flags, opposite 'hoist'"]]]
      [:li "allow fimbriation for cottises"]
      [:li "new charge detail view, separating the original SVG view from the preview"]
      [:li "improved blazonry editor, it understands more concepts now and there's a Discord bot that can use it"]
      [:li "a few new tincture modifiers: flagged, feathered"]
      [:li "a few more tooltips to help with the UI"]
      [:li "some modals for login/logout/confirmation were buggy, that has been fixed"]
      [:li "various bugfixes"]]]]

   [:h3 "2022-04-14 - Change of 'origin'/'anchor' naming"]
   [:div.release-row
    [:div.info
     [:p "The names for the two points used to position and orient partitions, ordinaries, and charges, have been renamed."]
     [:ul
      [:li [:strong "anchor"] " (previously " [:em "origin"] "): is the main point an element is positioned with."]
      [:li [:strong "orientation"] " (previously " [:em "anchor"] "): is the point an element orients itself towards, but it can also be a fixed angle."]]
     [:p "This might be confusing for a while for anyone already used to the old names, but I think it more accurately describes these concepts and simplifies using them. I hope it won't cause too much trouble."]]]

   [:h3 "2022-04-07 - Blazonry reader, translations, bordures with line styles, metadata"]
   [:div.release-row
    [:div.info
     [:p "The main new feature is a blazonry reader, you can click on the pen nib next to any field to invoke it or click the dedicated button to create new arms from a blazon. Features include:"]
     [:ul
      [:li "immediate rendering of the blazon, feedback if a word is not understood"]
      [:li "auto completion suggestions for any incomplete or malformed blazon"]
      [:li "support for all charges added by the community, your blazonry reader will understand all publically available charges and your private ones"]]
     [:p "supported concepts:"]
     [:ul
      [:li "partitions with sub fields and line styles, e.g. 'quartered i. and iv. azure, ii. and iii. or'"]
      [:li "ordinaries with line styles, fimbriation, and cottising, e.g. 'or, a fess wavy fimbriared gules argent'"]
      [:li "humetty/voided ordinaries, e.g. 'or, a pale humetty voided azure'"]
      [:li "ordinary groups, e.g. 'or, three barrulets gules'"]
      [:li "charges and charge groups with fimbriation, e.g. 'or, in chief three estoiles of 8 points sable', 'or, 12 roundels sable in orle' (or 'in annullo', 'palewise', etc.)"]
      [:li "semy, e.g. 'azure semy fleur-de-lis or'"]
      [:li "tincture/field referencing, e.g. 'per pale or and azure, a fess azure charged with a roundel of the field' (or 'of the first')"]
      [:li "various modifiers for ordinaries and charges that support it, e.g. 'reversed', 'mirrored', 'truncated', 'throughout', 'enhanced', etc."]]
     [:p "Additional changes and new features:"]
     [:ul
      [:li "the site now has a language selection, so far only English and German are really populated, but any help is welcome at "
       [:a {:href "https://crowdin.com/project/heraldicon"
            :target "_blank"} "Crowdin"]]
      [:li "pages and dialogs that list objects now have preview images and better filtering options"]
      [:li "bordures and orles can have line styles now"]
      [:li "furs now have an option to offset and scale them"]
      [:li "there's a dedicated star/mullet ordinary now, which allows an arbitrary number of points and wavy rays (to make an estoile)"]
      [:li "users can have an avatar now, for the moment it uses the Gravatar ("
       [:a {:href "https://gravatar.com"
            :target "_blank"} "Gravatar"] ") for the user's email address"]
      [:li "more escutcheon options, in particular the flag shape now can be used with all the usual and arbitrary aspect ratios, and it can have a configurable swallow tail"]
      [:li "allow arbitrary metadata for charges, arms, collections, which is considered for searching"]
      [:li "various element options allow a wider range of values and/or got better defaults"]
      [:li "performance improvements and bugfixes"]]]
    [:div
     (release-image "/img/2022-04-07-release-update.png")]]

   [:h3 "2022-01-09 - Happy new year!"]
   [:div.release-row
    [:div.info
     [:p "The site has a new name: " [:b "Heraldicon"]]
     [:p "Since there is at least one other group and a well-known heraldic artist who already use the name 'Digital Heraldry', the old name collided somewhat with them. "
      "'Heraldry' also is rather generic on its own, making it hard to look for it among many heraldry-related websites."]
     [:p "I hope the new name identifies the project more directly and is easier to find if anyone wants to refer to the website."]
     [:p "Note: all export links remain unchanged and all links to the old website should redirect to the new website. Let me know if you see any problems after the move. :)"]]]

   [:h3 "2021-11-25 - Bordure/orle, in orle charge groups, chevronny"]
   [:div.release-row
    [:div.info
     [:p "Changes and new features:"]
     [:ul
      [:li "Bordures and orles"
       [:ul
        [:li "there now are bordure and orle ordinaries with arbitrary thickness/distance to the edge"]
        [:li "so far they don't allow line styles other than straight, which follows the surrounding field edge"]]]

      [:li "Humetty/couped and voided ordinaries"
       [:ul
        [:li "ordinaries can now be made humetty/couped"]
        [:li "ordinaries can now be voided, this also honours the line style of the surrounding ordinary"]
        [:li "ordinaries also can be humetty/couped and voided"]]]

      [:li "Charge groups can be arranged in orle"
       [:ul
        [:li "this allows arranging N charges in orle, based on the surrounding field edge"]
        [:li "use the distance and offset arguments to adjust where exactly the charges end up"]]]

      [:li "Others"
       [:ul
        [:li "chevronny now is a field partition, configurable in most aspects, like other field partitions"]
        [:li "fur fields now have an additional 'pattern scaling' option to scale, you've guessed it, the pattern, "
         "which is useful for artistic preferences or in charges, where smaller ermine spots make more sense"]
        [:li "fields are clickable again in the rendering to select them more easily"]
        [:li "landscapes can now be added if raster graphics are embedded in an SVG and uploaded as charge, this also can be used for diapering"]
        [:li "a new community theme 'Content Cranium' and a silly theme that transitions through ALL themes"]
        [:li "there are a few more escutcheon choices for Norman shields and flag variants"]
        [:li "the interface should be MUCH faster now due to a rewrite of large parts of it"]
        [:li "there's a dedicated "
         [:a {:href "https://discord.gg/EGbMW8dth2" :target "_blank"} "Discord"]
         " server now, where my username is " [:em "or#5915"]]]]

      [:li "Known issues"
       [:ul
        [:li "the path for the bordure/orle and charge group in orle positions sometimes can have unexpected glitches, if that happens try to resize it a little or change the distance to the field edge"]
        [:li "charges sometimes block underlying fields when trying to click them, that's a technical issue with the way the browser works, hopefully it can be addressed someday"]]]]]

    [:div
     (release-image "/img/2021-11-25-release-update.png")]]

   [:h3 "2021-10-25 - Supporters, compartments, mantling, charge library improvements"]
   [:div.release-row
    [:div.info
     [:p "Changes and new features:"]
     [:ul
      [:li "Supporters, compartments, mantling"
       [:ul
        [:li "ornaments around the shield can now be added"]
        [:li "there are some default charges for supporters, compartment, and mantling"]
        [:li "any other existing charge or new custom charges can be used for this"]
        [:li "a mechanism allows rendering these charges behind and in front of the shield (see next point)"]]]

      [:li "charge library interface improvements"
       [:ul
        [:li "charges intended as ornaments can contain colour(s) that designate areas that separate background and foreground of the charge, so the shield can be rendered between them"]
        [:li "the charge library interface now allows highlighting colours to easily see which area of the charge is affected by configuring it"]
        [:li "a new shading option is available now, which can qualify colours with the level of shading it represents, so no alpha-transparency shadow/highlight has to be added manually for many existing SVGs"]
        [:li "colours can also be sorted by colour, function, and shading modifier, making it easier to group them while editing"]]]
      [:li "Known issues"
       [:ul
        [:li "mottos/slogans were moved into 'ornaments', and the achievement positioning system has been rewritten, unfortunately a "
         "migration preserving their position wasn't feasible for all cases, this means that "
         [:b "your mottos/slogans might have moved"] " so best check them and fix it"]]]]]

    [:div
     (release-image "/img/2021-10-25-release-update.png")]]

   [:h3 "2021-09-26 - German translation, undo/redo, quarter/canton/point ordinaries, fretty"]
   [:div.release-row
    [:div.info
     [:p "Changes and new features:"]
     [:ul
      [:li "German translation"
       [:ul
        [:li "the entire interface is now available in German"]
        [:li "News and About section aren't translated for the time being, might do that at some point as well"]
        [:li "there might be some translations that are not ideal or awkward the way they are used, email me if you find such cases! :)"]]]

      [:li "all forms now support undo/redo"]
      [:li "added the quarter/canton ordinary"]
      [:li "added the point ordinary"]
      [:li "added fretty"]
      [:li "added more flag aspect ratios"]
      [:li "Bugfixes"
       [:ul
        [:li "the environments for many ordinaries and subfields were broken (they still need some work, but now most of them should be reasonable)"]
        [:li "a bunch of minor things in the UI and rendering"]]]]]]

   [:h3 "2021-08-20 - helms/crests, mottos/slogans, ribbon editor + library"]
   [:div.release-row
    [:div.info
     [:p "Changes and new features:"]
     [:ul
      [:li "Helms/crests"
       [:ul
        [:li "helmets, torses, crests or any combination of them can now be added"]
        [:li "multiple helms are supported next to each other, they auto resize"]
        [:li "for the helmets three new tinctures were introduced: light, medium, dark, but a helmet is just a charge, so it has a normal field that allows any other modifications of a field"]]]

      [:li "Ribbons and mottos/slogans"
       [:ul
        [:li "a ribbon library can now be used to (hopefully easily) create dynamic ribbons to be used with mottos and slogans"]
        [:li "ribbons try to be reusable for many cases, things like thickness, size, the split of the ribbon's ends, various text properties for its segments can be changed in the arms where they are used"]
        [:li "mottos/slogans can live below/above the escutcheon (or overlap it) and be tinctured (foreground, background, text)"]]]

      [:li "the escutcheon can now be rotated freely from -45° to 45°, helms will settle at the respective top corner"]
      [:li "the SVG/PNG export is improved, now using a headless Chromium in the backend to generate pretty much the same as the preview on the page"]
      [:li "the PNG export yields a higher resolution"]
      [:li "charges can be masked vertically (top or bottom) to be able to make demi-charges that are issuant from behind torses or ordinaries or other charges, as can be seen in the examples in the crest and the lion behind a thin fess"]]

     [:p "Known issues:"]
     [:ul
      [:li "helms and mottos are labeled 'alpha', because they are bound to change still, positioning might change in the future"]
      [:li "the ribbon editor definitely needs more work to be more user friendly or have better explanation"]
      [:li "the default position for helmet/torse/crest is somewhat sensible only for the default helmet and torse right now, for others it needs to be tweaked manually, this should improve in the future"]
      [:li "similarly slogans are positioned rather high above the escutcheon to make room for helm crests, even if there aren't any; again this should be smarter in the future"]
      [:li "fonts are embedded in exported SVGs, some editors/viewers won't display them correctly due to the different way some SVG markup is interpreted, Chrome should work"]]]

    [:div
     (release-image "/img/2021-08-20-release-update.png")]]

   [:h3 "2021-07-30 - UI rewrite, validation system, social media sharing"]
   [:div.release-row
    [:div.info
     [:p "Changes and new features:"]
     [:ul
      [:li "UI rewrite"
       [:ul
        [:li "the nested components are gone, as they were confusing, hard to navigate, and caused annoying scrolling issues"]
        [:li "separate navigation tree and form for selected components"]
        [:li "big speed improvements, a lot of the UI interactions now take 1-5ms, where previously 100-200ms were commonplace"]
        [:li "the layout works a bit better on mobile devices in landscape more, but the target environment still is Chrome on a desktop"]
        [:li "the cogs next to options allow an easy jump back to the default value or in some cases the value inherited from other components"]
        [:li "sliders now have a text field next to it to update values with specific values"]]]

      [:li "a validation system to warn about components that might break rule of tincture and potentially other issues"]
      [:li "improved blazonry, it now makes more of an attempt of reporting the used charges in charge groups, and it includes fimbriation"]
      [:li "SVG/PNG exports now contain a short URL to reference the arms on Heraldicon"]
      [:li "pall ordinaries now can be added, with different fimbriation for each line and arbitrary issuing from anywhere"]]]

    [:div
     (release-image "/img/2021-07-30-release-update.png")]]

   [:h3 "2021-06-12 - Charge groups, public arms, collections, users"]
   [:div.release-row
    [:div.info
     [:p "New features:"]
     [:ul
      [:li "support for charge groups, charges can be arranged in rows, columns, grids or arcs/circles"]
      [:li "various charge group presets for the most common arrangements"]
      [:li "users can now be browsed"]
      [:li "public arms and collections can now be browsed"]
      [:li "tincture modifiers 'orbed' and 'illuminated' added"]]
     [:p "Known issues:"]
     [:ul
      [:li "many charges render slowly"]
      [:li "charge groups should take the surrounding field better into account in some situations, more work needed"]
      [:li "charge group presets inspired by ordinaries use the grid and might not align perfectly with ordinaries, more work needed"]]]
    [:div
     (release-image "/img/2021-06-12-release-update.png")]]

   [:h3 "2021-06-06 - Collections, tags, filter"]
   [:div.release-row
    [:div.info
     [:p "New features:"]
     [:ul
      [:li "support for collections, which can be used to group existing arms, to create rolls of arms, group drafts, display arms for a specific topic, etc."]
      [:li "arms, charges, and collections now also can be tagged arbitrarily, and these tags can then be used to filter for them or annotate some notes"]
      [:li "there are now separate options for escutcheon shadow and outline"]
      [:li "text in collections can be rendered with various fonts"]]
     [:p "Known issues:"]
     [:ul
      [:li "large collections might render slowly, now that there are multiple arms being rendered on the fly, it shows that some parts of the rendering engines are not optimized yet"]]]
    [:div
     (release-image "/img/2021-06-06-release-update.png")]]

   [:h3 "2021-05-09 - Cottising, labels, semy"]
   [:div.release-row
    [:div.info
     [:p "New features:"]
     [:ul
      [:li "support for cottising, cottises can have their own line styles, fimbriation, and follow the parent ordinary's alignment"]
      [:li "labels ordinaries can now be added, they can be full length, truncated/couped, and support various emblazonment options"]
      [:li "the chevron interface has changed a bit again, now using the anchor system as well, so they can be issuant from anywhere"]
      [:li "semys with arbitrary charges are now possible, charges can be resized, rotated, the whole semy pattern layout also can be configured"]
      [:li "new line styles: rayonny (flaming), rayonny (spiked), wolf toothed"]
      [:li "lines can be aligned to their top, bottom, or middle"]]]
    [:div
     (release-image "/img/2021-05-09-release-update.png")]]

   [:h3 "2021-03-31 - Embowed/enarched, nonrepeating line styles"]
   [:div.release-row
    [:div.info
     [:p "A big refactoring line styles, allowing line styles that are not pattern-based but extend across the full length of the line."]
     [:ul
      [:li "support of full-length line styles, such as bevilled, angled, and enarched"]
      [:li "new pattern-based line styles, such as potenty, embattled-grady, embattled-in-crosses, nebuly, fir-twigged, fir-tree-topped, thorny"]
      [:li "gore ordinaries, which also uses the enarched line style, but can use all other line styles as well"]
      [:li "bug fixes"]]]
    [:div
     (release-image "/img/2021-03-31-release-update.png")]]

   [:h3 "2021-03-16 - Chevron and pile"]
   [:div.release-row
    [:div.info
     [:p "A total refactoring of angular alignment necessary for chevron and pile variants."]
     [:ul
      [:li "chevron ordinary and division variants issuant from chief, dexter, and sinister"]
      [:li "pile ordinary and division variants issuant from anywhere, pointing at specific points or the opposite edge"]
      [:li "edge detection of the surrounding field for piles is dynamic and works for any shape and orientation"]
      [:li "orientation based on an " [:b "origin"] " and an " [:b "anchor"]
       ", which allows precise construction of bends, chevrons, saltires, piles, etc."]
      [:li "new concept of " [:b "alignment"] " allows aligning ordinary edges with the chosen origin/anchor, "
       "not just the middle axis"]
      [:li "more support of arbitrary SVGs for the charge library"]
      [:li "WappenWiki and Wikimedia licensing presets for charge attribution"]
      [:li "bug fixes"]]]
    [:div
     (release-image "/img/2021-03-16-release-update.png")]]

   [:h3 "2021-03-07 - Paly and fimbriation"]
   [:div.release-row
    [:div.info
     [:p "The main new features are:"]
     [:ul
      [:li "paly/barry/bendy/chequy/lozengy field divisions with configurable layout"]
      [:li "quarterly division of MxN fields"]
      [:li "vairy treatment and its variations"]
      [:li "potenty treatment and its variations"]
      [:li "papellony and masoned treatment"]
      [:li "line fimbriation (single and double) with configurable thickness, which can handle intersecting with itself and optional outlines"]
      [:li "charge fimbriation with configurable thickness"]
      [:li "shininess and shield textures, which also can be used as displacement maps for various effects"]
      [:li "optional alpha transparency shadow/highlight in charges, which is applied dynamically after rendering "
       "the charge field, i.e. it works with any tincture or treatment or division of the charge's field"]
      [:li "bug fixes"]]]
    [:div
     (release-image "/img/2021-03-07-release-update.png")]]

   [:h3 "2021-02-08 - First release"]
   [:div.release-row
    [:div.info
     [:p "Following a browser-only prototype, this site now has a backend, where users can build a public "
      "and private library of charges and arms."]
     [:p
      "Features include:"]
     [:ul
      [:li "various escutcheons with their own relevant points, e.g. fess, honour, nombril, etc."]
      [:li "several divisions"]
      [:li "several ordinaries"]
      [:li "several line styles"]
      [:li "some common charge shapes"]
      [:li "lion and wolf charges in various attitudes"]
      [:li "counterchanged ordinaries and charges"]
      [:li "ermine-like furs"]
      [:li "dimidiation"]
      [:li "very basic blazoning of the constructed arms"]
      [:li "tincture themes, including hatching"]
      [:li "licensing information and attribution can be given for charges and arms, and indeed is required to make either public"]
      [:li "SVG/PNG export of saved arms; the saving is necessary for proper attribution"]]]
    [:div.info
     (release-image "/img/2021-02-08-release-update.png")]]])

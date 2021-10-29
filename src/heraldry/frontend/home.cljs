(ns heraldry.frontend.home
  (:require
   [heraldry.static :as static]
   [re-frame.core :as rf]))

(defn release-image [img-src]
  (let [src (static/static-url img-src)]
    [:a {:href src
         :target "_blank"}
     [:img {:style {:width "100%"}
            :src src
            :alt "release update overview"}]]))

(defn view []
  (rf/dispatch [:set-title "Home"])
  [:div
   {:style {:padding "10px"
            :text-align "justify"
            :min-width "30em"
            :max-width "60em"
            :margin "auto"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:h2 "News"]
    [:p "In many cases new features are rolled out incrementally, without big release. But now and then I'll "
     "group some features and new development and post an update here, so it is easy to stay informed."]]

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
      [:li "SVG/PNG exports now contain a short URL to reference the arms on heraldry.digital"]
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

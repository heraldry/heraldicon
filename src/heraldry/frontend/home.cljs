(ns heraldry.frontend.home)

(defn release-image [img-src]
  [:a {:href img-src
       :target "_blank"}
   [:img {:style {:width "calc(90% - 1em)"
                  :margin-left "1em"}
          :src img-src
          :alt "release update overview"}]])

(defn view []
  [:div {:style {:padding "10px"}}
   [:h2 "News"]
   [:p "In many cases new features are rolled out incrementally, without big release. But now and then I'll "
    "group some features and new development and post an update here, so it is easy to stay informed."]

   [:h3 "2021-06-12 - Charge groups, public arms, collections, users"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
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
    [:div.pure-u-1-3
     (release-image "/img/2021-06-12-release-update.png")]]

   [:h3 "2021-06-06 - Collections, tags, filter"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
     [:p "New features:"]
     [:ul
      [:li "support for collections, which can be used to group existing arms, to create rolls of arms, group drafts, display arms for a specific topic, etc."]
      [:li "arms, charges, and collections now also can be tagged arbitrarily, and these tags can then be used to filter for them or annotate some notes"]
      [:li "there are now separate options for escutcheon shadow and outline"]
      [:li "text in collections can be rendered with various fonts"]]
     [:p "Known issues:"]
     [:ul
      [:li "large collections might render slowly, now that there are multiple arms being rendered on the fly, it shows that some parts of the rendering engines are not optimized yet"]]]
    [:div.pure-u-1-3
     (release-image "/img/2021-06-06-release-update.png")]]

   [:h3 "2021-05-09 - Cottising, labels, semy"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
     [:p "New features:"]
     [:ul
      [:li "support for cottising, cottises can have their own line styles, fimbriation, and follow the parent ordinary's alignment"]
      [:li "labels ordinaries can now be added, they can be full length, truncated/couped, and support various emblazonment options"]
      [:li "the chevron interface has changed a bit again, now using the anchor system as well, so they can be issuant from anywhere"]
      [:li "semys with arbitrary charges are now possible, charges can be resized, rotated, the whole semy pattern layout also can be configured"]
      [:li "new line styles: rayonny (flaming), rayonny (spiked), wolf toothed"]
      [:li "lines can be aligned to their top, bottom, or middle"]]]
    [:div.pure-u-1-3
     (release-image "/img/2021-05-09-release-update.png")]]

   [:h3 "2021-03-31 - Embowed/enarched, nonrepeating line styles"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
     [:p "A big refactoring line styles, allowing line styles that are not pattern-based but extend across the full length of the line."]
     [:ul
      [:li "support of full-length line styles, such as bevilled, angled, and enarched"]
      [:li "new pattern-based line styles, such as potenty, embattled-grady, embattled-in-crosses, nebuly, fir-twigged, fir-tree-topped, thorny"]
      [:li "gore ordinaries, which also uses the enarched line style, but can use all other line styles as well"]
      [:li "bug fixes"]]]
    [:div.pure-u-1-3
     (release-image "/img/2021-03-31-release-update.png")]]

   [:h3 "2021-03-16 - Chevron and pile"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
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
    [:div.pure-u-1-3
     (release-image "/img/2021-03-16-release-update.png")]]

   [:h3 "2021-03-07 - Paly and fimbriation"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
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
    [:div.pure-u-1-3
     (release-image "/img/2021-03-07-release-update.png")]]

   [:h3 "2021-02-08 - First release"]
   [:div.pure-g
    [:div.pure-u-2-3 {:style {:text-align "justify"
                              :min-width "30em"}}
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
    [:div.pure-u-1-3
     (release-image "/img/2021-02-08-release-update.png")]]])

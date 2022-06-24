(ns heraldicon.frontend.ui.element.blazonry-editor.help
  (:require
   [re-frame.core :as rf]))

(def blazonry-examples
  [["partitions with sub fields and line styles"
    ["per pale indented or and azure"
     "tierced per pall gules, argent and or"
     "quartered (or, an orle gules), (azure, a fess argent)"
     "vairy argent and azure"
     "potenty of 6x10 or and gules"]]
   ["ordinaries with line styles, fimbriation, and cottising"
    ["azure, a fess wavy or"
     "or, a bend chequy of 12x3 tracts azure and argent"
     "azure, a chevronnel enhanced inverted or"
     "azure, a pall fimbriated or gules"
     "azure, a label of 5 points dovetailed truncated gules"
     "azure, a fess or cottised argent and or"]]
   ["humetty/voided ordinaries"
    ["azure, a fess humetty or"
     "azure, a pale voided or"
     "azure, a pall couped and voided or"]]
   ["ordinary groups"
    ["azure, three barrulets or"
     "azure, double tressures engrailed or"
     "azure, a bordure engrailed or"
     "azure, 3 piles throughout or"]]
   ["charges with fimbriation"
    ["azure, a star of 6 points fimbriated or sable"
     "azure, a lion or, langued gules, armed argent"
     "azure, a lion sejant reversed or"]]
   ["charge groups"
    ["azure, a chief argent charged with 3 stars sable"
     "per pale gules, or, twelve stars counterchanged in annullo"
     "azure, 10 roundels or 4 3 2 1"
     "azure, 8 stars argent in orle"]]
   ["semy"
    ["azure semy fleur-de-lis or"
     "or semé of 6x8 stars gules"]]
   ["tincture referencing"
    ["tierced per fess azure, or, and argent, a pallet of the third, a pallet of the second, a pallet of the first"
     "or, chief sable charged with three stars of the field"
     "chequy or and gules, chief sable charged with three stars of the field"
     "or, chief enhanced sable, a mascle per pale of the same and gules"]]])

(defn help []
  [:div.blazonry-editor-help
   {:style {:width "25em"
            :overflow-y "scroll"}}
   [:p "This blazonry parser is a work in progress. While it already can parse a lot of blazons, there is still a lot of work to do. Once you apply the result you can edit it further in the main interface."]
   [:p "Some things it supports already:"]
   [:ul
    [:li "English blazonry"]
    [:li "TAB auto completes first suggestion"]
    (into [:<>]
          (map (fn [[group-name blazons]]
                 [:li group-name
                  (into [:ul]
                        (map (fn [blazon]
                               [:li [:span.blazon-example
                                     {:on-click #(rf/dispatch [:heraldicon.frontend.ui.element.blazonry-editor/set-blazon blazon])}
                                     blazon]]))
                        blazons)]))
          blazonry-examples)]
   [:p "Some things that still need work or have known issues:"]
   [:ul
    [:li "blazonry in other languages"]
    [:li "explicit charge positioning, e.g. 'in chief', 'in base'"]
    [:li "charge/ordinary arrangement in relation to each other, e.g. 'between'"]]])

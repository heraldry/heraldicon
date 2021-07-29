(ns heraldry.coat-of-arms.tincture.core
  (:require [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.tincture.pattern :as pattern]
            [heraldry.coat-of-arms.tincture.theme :as theme]
            [heraldry.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(def themes
  [["General"
    ["WappenWiki (default)" :wappenwiki theme/theme-wappenwiki]
    ["Web" :theme-web theme/theme-web]
    ["RAL Traffic" :ral-traffic theme/theme-ral-traffic]]
   ["Wikipedia"
    ["Wikipedia default" :wikipedia-default theme/theme-wikipedia-default]
    ["Wikipedia web" :wikipedia-web theme/theme-wikipedia-web]
    ["Wikipedia Bajuvarian" :wikipedia-bajuvarian theme/theme-wikipedia-bajuvarian]
    ["Wikipedia Brandenburg" :wikipedia-brandenburg theme/theme-wikipedia-brandenburg]
    ["Wikipedia Württemberg" :wikipedia-wurttemberg theme/theme-wikipedia-wuerttemberg]
    ["Wikipedia France" :wikipedia-france theme/theme-wikipedia-france]
    ["Wikipedia Hungary" :wikipedia-hungary theme/theme-wikipedia-hungary]
    ["Wikipedia Spain" :wikipedia-spain theme/theme-wikipedia-spain]
    ["Wikipedia Sweden" :wikipedia-sweden theme/theme-wikipedia-sweden]
    ["Wikipedia Switzerland" :wikipedia-switzerland theme/theme-wikipedia-switzerland]]
   ["Community"
    ["CMwhyK" :community-cmwhyk theme/theme-community-cmwhyk]
    ["Cotton Candy" :community-cotton-candy theme/theme-community-cotton-candy]
    ["Crystal Gems" :community-crystal-gems theme/theme-community-crystal-gems]
    ["Home World" :community-home-world theme/theme-community-home-world]
    ["Jewelicious" :community-jewelicious theme/theme-community-jewelicious]
    ["Main Seven" :community-main-seven theme/theme-community-main-seven]
    ["Mother Earth" :community-mother-earth theme/theme-community-mother-earth]
    ["Pastell Puffs" :community-pastell-puffs theme/theme-community-pastell-puffs]
    ["Pretty Soldier" :community-pretty-soldier theme/theme-community-pretty-soldier]
    ["Rainbow Groom" :community-rainbow-groom theme/theme-community-rainbow-groom]
    ["The Monet Maker" :community-the-monet-maker theme/theme-community-the-monet-maker]
    ["Van Goes Vroem" :community-van-goes-vroem theme/theme-community-van-goes-vroem]]])

(def default-theme
  :wappenwiki)

(def theme-choices
  (->> themes
       (map (fn [[group-name & items]]
              (vec (concat [group-name] (->> items
                                             (map (fn [[display-name key _]]
                                                    [display-name key])))))))
       vec))

(def theme-map
  (util/choices->map theme-choices))

(def theme-data-map
  (->> themes
       (map (fn [[_group-name & items]]
              (->> items
                   (map (fn [[_ key colours]]
                          [key colours])))))
       (apply concat)
       (into {})))

(def choices
  [["Metal"
    ["None" :none]
    ["Argent" :argent]
    ["Or" :or]]
   ["Colour"
    ["Azure" :azure]
    ["Gules" :gules]
    ["Purpure" :purpure]
    ["Sable" :sable]
    ["Vert" :vert]]
   ["Fur"
    ["Ermine" :ermine]
    ["Ermines" :ermines]
    ["Erminois" :erminois]
    ["Pean" :pean]]
   ["Stain"
    ["Sanguine" :sanguine]
    ["Murrey" :murrey]
    ["Tenné" :tenne]]])

(def tincture-map
  (util/choices->map choices))

(def fixed-tincture-choices
  (concat [["None (can be changed)" :none]
           ["Proper" :proper]]
          choices))

(defn kind [tincture]
  (cond
    (#{:none :mixed} tincture) :mixed
    (#{:argent :or} tincture) :metal
    (#{:ermine :ermines :erminois :pean} tincture) :fur
    :else :colour))

(def fixed-tincture-map
  (util/choices->map fixed-tincture-choices))

(defn lookup-colour [tincture theme]
  (let [theme-colours (merge
                       (get theme-data-map default-theme)
                       (get theme-data-map theme))]
    (get theme-colours tincture)))

(def ermine
  ["ermine" :argent :sable])

(def ermines
  ["ermines" :sable :argent])

(def erminois
  ["erminois" :or :sable])

(def pean
  ["pean" :sable :or])

(def furs
  {:ermine ermine
   :ermines ermines
   :erminois erminois
   :pean pean})

(defn pick [tincture {:keys [tincture-mapping] :as context}]
  (let [mode (interface/render-option :mode context)
        theme (interface/render-option :theme context)
        tincture (get tincture-mapping tincture tincture)]
    (cond
      (= tincture :none) "url(#void)"
      (get furs tincture) (let [[id _ _] (get furs tincture)]
                            (str "url(#" id ")"))
      (= mode :hatching) (or
                          (hatching/get-for tincture)
                          "#888")
      :else (or (lookup-colour tincture theme)
                (get furs tincture)
                "url(#void)"))))

(defn patterns [theme]
  (into
   [:<>
    pattern/void
    pattern/selected]
   (for [[id background foreground] (vals furs)]
     (pattern/ermine-base
      id
      (lookup-colour background theme)
      (lookup-colour foreground theme)))))

(defn tinctured-field [tincture-path context & {:keys [mask-id
                                                       transform]}]
  (let [tincture (interface/get-sanitized-data tincture-path context)]
    (conj (if mask-id
            [:g {:mask (str "url(#" mask-id ")")}]
            [:<>])
          [:rect {:x -500
                  :y -500
                  :width 1100
                  :height 1100
                  :transform transform
                  :fill (pick tincture context)}])))

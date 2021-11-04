(ns heraldry.coat-of-arms.tincture.core
  (:require
   [clojure.string :as s]
   [heraldry.coat-of-arms.hatching :as hatching]
   [heraldry.coat-of-arms.tincture.pattern :as pattern]
   [heraldry.coat-of-arms.tincture.theme :as theme]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def themes
  [[strings/general
    [{:en "WappenWiki (default)"
      :de "WappenWiki (Default)"} :wappenwiki theme/theme-wappenwiki]
    ["Web" :theme-web theme/theme-web]
    ["RAL Traffic" :ral-traffic theme/theme-ral-traffic]
    [{:en "All of them!"
      :de "Alle!"} :all theme/theme-all]]
   ["Wikipedia"
    [{:en "Wikipedia default"
      :de "Wikipedia Default"} :wikipedia-default theme/theme-wikipedia-default]
    [{:en "Wikipedia web"
      :de "Wikipedia Web"} :wikipedia-web theme/theme-wikipedia-web]
    [{:en "Wikipedia Bajuvarian"
      :de "Wikipedia Bayern"} :wikipedia-bajuvarian theme/theme-wikipedia-bajuvarian]
    [{:en "Wikipedia Brandenburg"
      :de "Wikipedia Brandenburg"} :wikipedia-brandenburg theme/theme-wikipedia-brandenburg]
    [{:en "Wikipedia Württemberg"
      :de "Wikipedia Württemberg"} :wikipedia-wurttemberg theme/theme-wikipedia-wuerttemberg]
    [{:en "Wikipedia France"
      :de "Wikipedia Frankreich"} :wikipedia-france theme/theme-wikipedia-france]
    [{:en "Wikipedia Hungary"
      :de "Wikipedia Ungarn"} :wikipedia-hungary theme/theme-wikipedia-hungary]
    [{:en "Wikipedia Spain"
      :de "Wikipedia Spanien"} :wikipedia-spain theme/theme-wikipedia-spain]
    [{:en "Wikipedia Sweden"
      :de "Wikipedia Schweden"} :wikipedia-sweden theme/theme-wikipedia-sweden]
    [{:en "Wikipedia Switzerland"
      :de "Wikipedia Schweiz"} :wikipedia-switzerland theme/theme-wikipedia-switzerland]]
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
  [[{:en "Metal"
     :de "Metall"}
    [strings/void :none]
    [{:en "Argent"
      :de "Silber"} :argent]
    [{:en "Or"
      :de "Gold"} :or]]
   [{:en "Colour"
     :de "Farbe"}
    [{:en "Azure"
      :de "Blau"} :azure]
    [{:en "Gules"
      :de "Rot"} :gules]
    [{:en "Purpure"
      :de "Purpur"} :purpure]
    [{:en "Sable"
      :de "Schwarz"} :sable]
    [{:en "Vert"
      :de "Grün"} :vert]]
   [{:en "Fur"
     :de "Pelzwerk"}
    [{:en "Ermine"
      :de "Hermelin"} :ermine]
    [{:en "Ermines"
      :de "Gegenhermelin"} :ermines]
    [{:en "Erminois"
      :de "Goldhermelin"} :erminois]
    [{:en "Pean"
      :de "Gegenhermelin in Gold"} :pean]]
   ["Stain"
    ["Sanguine" :sanguine]
    ["Murrey" :murrey]
    ["Tenné" :tenne]]
   [{:en "Helmet"
     :de "Helm"}
    [{:en "Light"
      :de "Hell"} :helmet-light]
    ["Medium" :helmet-medium]
    [{:en "Dark"
      :de "Dunkel"} :helmet-dark]]])

(def tincture-map
  (util/choices->map choices))

(defn translate-tincture [keyword]
  (tincture-map keyword (util/translate keyword)))

(def fixed-tincture-choices
  (concat [[{:en "None (can be changed)"
             :de "Keine (kann verändert werden)"} :none]
           [{:en "Proper"
             :de "Natürlich"} :proper]]
          choices))

(defn kind [tincture]
  (cond
    (#{:none :mixed} tincture) :mixed
    (#{:argent :or} tincture) :metal
    (#{:ermine :ermines :erminois :pean} tincture) :fur
    (-> tincture
        name
        (s/starts-with? "helmet")) :special
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

(def special
  {:helmet-light "#d8d8d8"
   :helmet-medium "#989898"
   :helmet-dark "#585858"})

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
                (get special tincture)
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

(defn tinctured-field [tincture-path {:keys [tincture-mapping] :as context} & {:keys [mask-id
                                                                                      transform]}]
  (let [tincture (interface/get-sanitized-data tincture-path context)
        theme (interface/render-option :theme context)
        theme (if (and (:svg-export? context)
                       (= theme :all))
                :wappenwiki
                theme)
        effective-tincture (get tincture-mapping tincture tincture)
        [colour animation] (if (and (= theme :all)
                                    (-> theme-data-map
                                        (get :wappenwiki)
                                        (get effective-tincture)))
                             [nil (str "all-theme-transition-" (name effective-tincture))]
                             [(pick tincture context) nil])]
    (conj (if mask-id
            [:g {:mask (str "url(#" mask-id ")")}]
            [:<>])
          [:rect {:x -500
                  :y -500
                  :width 1100
                  :height 1100
                  :transform transform
                  :fill colour
                  :style (when animation
                           {:animation (str animation " linear 20s infinite")})}])))

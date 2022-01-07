(ns heraldry.coat-of-arms.tincture.core
  (:require
   [clojure.string :as s]
   [heraldry.coat-of-arms.hatching :as hatching]
   [heraldry.coat-of-arms.tincture.pattern :as pattern]
   [heraldry.coat-of-arms.tincture.theme :as theme]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(def themes
  [[:string.theme.group/general
    [:string.theme/wappenwiki :wappenwiki theme/theme-wappenwiki]
    [:string.theme/web :web theme/theme-web]
    [:string.theme/ral-traffic :ral-traffic theme/theme-ral-traffic]
    [:string.theme/all :all theme/theme-all]]
   ["Wikipedia"
    [:string.theme/wikipedia-default :wikipedia-default theme/theme-wikipedia-default]
    [:string.theme/wikipedia-web :wikipedia-web theme/theme-wikipedia-web]
    [:string.theme/wikipedia-bajuvarian :wikipedia-bajuvarian theme/theme-wikipedia-bajuvarian]
    [:string.theme/wikipedia-brandenburg :wikipedia-brandenburg theme/theme-wikipedia-brandenburg]
    [:string.theme/wikipedia-wurttemberg :wikipedia-wurttemberg theme/theme-wikipedia-wuerttemberg]
    [:string.theme/wikipedia-france :wikipedia-france theme/theme-wikipedia-france]
    [:string.theme/wikipedia-hungary :wikipedia-hungary theme/theme-wikipedia-hungary]
    [:string.theme/wikipedia-spain :wikipedia-spain theme/theme-wikipedia-spain]
    [:string.theme/wikipedia-sweden :wikipedia-sweden theme/theme-wikipedia-sweden]
    [:string.theme/wikipedia-switzerland :wikipedia-switzerland theme/theme-wikipedia-switzerland]]
   [:string.theme.group/community
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
    ["Van Goes Vroem" :community-van-goes-vroem theme/theme-community-van-goes-vroem]
    ["Content Cranium" :community-content-cranium theme/theme-community-content-cranium]]])

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
  [[:string.tincture.group/metal
    [:string.tincture/none :none]
    [:string.tincture/argent :argent]
    [:string.tincture/or :or]]
   [:string.tincture.group/colour
    [:string.tincture/azure :azure]
    [:string.tincture/gules :gules]
    [:string.tincture/purpure :purpure]
    [:string.tincture/sable :sable]
    [:string.tincture/vert :vert]]
   [:string.tincture.group/fur
    [:string.tincture/ermine :ermine]
    [:string.tincture/ermines :ermines]
    [:string.tincture/erminois :erminois]
    [:string.tincture/pean :pean]]
   [:string.tincture.group/stain
    [:string.tincture/sanguine :sanguine]
    [:string.tincture/murrey :murrey]
    [:string.tincture/tenne :tenne]]
   [:string.tincture.group/helmet
    [:string.tincture/helmet-light :helmet-light]
    [:string.tincture/helmet-medium :helmet-medium]
    [:string.tincture/helmet-dark :helmet-dark]]])

(def tincture-map
  (util/choices->map choices))

(defn translate-tincture [keyword]
  (tincture-map keyword (util/translate keyword)))

(def fixed-tincture-choices
  (concat [[:string.tincture.special/none :none]
           [:string.tincture.special/proper :proper]]
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
                          "#eee")
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

(defn tinctured-field [{:keys [tincture-mapping
                               svg-export?
                               select-component-fn] :as context}

                       & {:keys [mask-id
                                 transform]}]
  (let [tincture (interface/get-sanitized-data (c/++ context :tincture))
        pattern-scaling (interface/get-sanitized-data (c/++ context :pattern-scaling))
        pattern-rotation (interface/get-sanitized-data (c/++ context :pattern-rotation))
        pattern-offset-x (interface/get-sanitized-data (c/++ context :pattern-offset-x))
        pattern-offset-y (interface/get-sanitized-data (c/++ context :pattern-offset-y))
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
          [:rect {:x -1000
                  :y -1000
                  :width 2000
                  :height 2000
                  :transform (cond-> transform
                               (and pattern-offset-x
                                    pattern-offset-y) (str "translate("
                                                           pattern-offset-x ","
                                                           pattern-offset-y ")")
                               pattern-scaling (str "scale(" pattern-scaling "," pattern-scaling ")")
                               pattern-rotation (str "rotate(" pattern-rotation ")"))
                  :fill colour
                  :on-click (when (and (not svg-export?)
                                       select-component-fn)
                              #(select-component-fn % context))
                  :style (merge
                          {:cursor "pointer"}
                          (when animation
                            {:animation (str animation " linear 20s infinite")}))}])))

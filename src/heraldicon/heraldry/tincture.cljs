(ns heraldicon.heraldry.tincture
  (:require
   [clojure.string :as s]
   [heraldicon.blazonry :as blazonry]
   [heraldicon.context :as c]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.render.hatching :as hatching]
   [heraldicon.render.theme :as theme]))

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
    [:string.tincture/helmet-dark :helmet-dark]]
   [:string.tincture.group/nontraditional
    [:string.tincture/bleu-celeste :bleu-celeste]
    [:string.tincture/brunatre :brunatre]
    [:string.tincture/buff :buff]
    [:string.tincture/carnation :carnation]
    [:string.tincture/cendree :cendree]
    [:string.tincture/copper :copper]
    [:string.tincture/orange :orange]
    [:string.tincture/rose :rose]
    [:string.tincture/white :white]]])

(def tincture-map
  (options/choices->map choices))

(defn translate-tincture [keyword]
  (tincture-map keyword (blazonry/translate keyword)))

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
  (options/choices->map fixed-tincture-choices))

(def ^:private ermine
  ["ermine" :argent :sable])

(def ^:private ermines
  ["ermines" :sable :argent])

(def ^:private erminois
  ["erminois" :or :sable])

(def ^:private pean
  ["pean" :sable :or])

(def furs
  {:ermine ermine
   :ermines ermines
   :erminois erminois
   :pean pean})

(def ^:private special
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
      :else (or (theme/lookup-colour tincture theme)
                (get special tincture)
                "url(#void)"))))

(defn tinctured-field [{:keys [tincture-mapping
                               svg-export?
                               select-component-fn
                               charge-preview?] :as context}

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
                                    (-> theme/theme-data-map
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
                              (js-event/handled
                               #(select-component-fn context)))
                  :style (merge
                          (when-not charge-preview?
                            {:cursor "pointer"})
                          (when animation
                            {:animation (str animation " linear 20s infinite")}))}])))

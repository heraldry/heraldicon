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
    [:string.tincture/murrey :murrey]
    [:string.tincture/sanguine :sanguine]
    [:string.tincture/tenne :tenne]]
   [:string.tincture.group/helmet
    [:string.tincture/helmet-light :helmet-light]
    [:string.tincture/helmet-medium :helmet-medium]
    [:string.tincture/helmet-dark :helmet-dark]]
   [:string.tincture.group/nontraditional
    [:string.tincture/amaranth :amaranth]
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

(defn pick [tincture context]
  (let [mode (interface/render-option :mode context)
        theme (interface/render-option :theme context)
        tincture-mapping (c/tincture-mapping context)
        tincture (get tincture-mapping tincture tincture)]
    (cond
      (= tincture :none) "url(#void)"
      (get furs tincture) (let [[id _ _] (get furs tincture)]
                            (str "url(#" id ")"))
      (= mode :hatching) (or
                          (hatching/get-for tincture)
                          "#eee")
      :else (or (theme/lookup-colour tincture theme)
                "url(#void)"))))

(defn tinctured-field [context & {:keys [mask-id
                                         transform]}]
  (let [{:keys [charge-preview?
                svg-export?
                select-component-fn
                enter-component-fn
                leave-component-fn]} (c/render-hints context)
        tincture (interface/get-sanitized-data (c/++ context :tincture))
        pattern-scaling (interface/get-sanitized-data (c/++ context :pattern-scaling))
        pattern-rotation (interface/get-sanitized-data (c/++ context :pattern-rotation))
        pattern-offset-x (interface/get-sanitized-data (c/++ context :pattern-offset-x))
        pattern-offset-y (interface/get-sanitized-data (c/++ context :pattern-offset-y))
        theme (interface/render-option :theme context)
        theme (if (and svg-export?
                       (= theme :all))
                :wappenwiki
                theme)
        tincture-mapping (c/tincture-mapping context)
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
          [:rect {:x (if svg-export?
                       -1000
                       -500)
                  :y (if svg-export?
                       -1000
                       -500)
                  :width (if svg-export?
                           2000
                           1100)
                  :height (if svg-export?
                            2000
                            1100)
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
                  :on-mouse-enter (when (and (not svg-export?)
                                             enter-component-fn)
                                    (js-event/handled
                                     #(enter-component-fn context)))
                  :on-mouse-leave (when (and (not svg-export?)
                                             enter-component-fn)
                                    (js-event/handled
                                     #(leave-component-fn context)))
                  :style (merge
                          (when-not charge-preview?
                            {:cursor "pointer"})
                          (when animation
                            {:animation (str animation " linear 20s infinite")}))}])))

(ns heraldry.coat-of-arms.line.fimbriation
  (:require
   [clojure.walk :as walk]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def type-choices
  [[strings/none :none]
   [{:en "Single"
     :de "Einfach"} :single]
   [{:en "Double"
     :de "Doppelt"} :double]])

(def type-map
  (util/choices->map type-choices))

(def alignment-choices
  [[{:en "Even"
     :de "Gleichmäßig"} :even]
   [{:en "Outside"
     :de "Außen"} :outside]
   [{:en "Inside"
     :de "Innen"} :inside]])

(def alignment-map
  (util/choices->map alignment-choices))

(def mode-option
  {:type :choice
   :choices type-choices
   :default :none
   :ui {:form-type :radio-select}})

(defn options [{:keys [path] :as context}]
  (let [line-kind (-> path drop-last last #{:opposite-line :extra-line})
        inherited (when line-kind
                    (let [line-fimbriation-context (-> context (c/-- 2) (c/++ :line :fimbriation))]
                      (options/sanitize (interface/get-raw-data line-fimbriation-context)
                                        (options line-fimbriation-context))))
        effective-mode-option (cond-> mode-option
                                inherited (assoc :default (:mode inherited)))
        mode (options/get-value (interface/get-raw-data (c/++ context :mode)) effective-mode-option)]
    (-> {:mode mode-option
         :ui {:label strings/fimbriation
              :form-type :fimbriation}}
        (cond->
          (#{:single
             :double} mode) (assoc :alignment {:type :choice
                                               :choices alignment-choices
                                               :default :even
                                               :ui {:label strings/alignment}}
                                   :corner {:type :choice
                                            :choices [[{:en "Round"
                                                        :de "Rund"} :round]
                                                      [{:en "Sharp"
                                                        :de "Spitz"} :sharp]
                                                      [{:en "Bevel"
                                                        :de "Abgeschnitten"} :bevel]]
                                            :default :sharp
                                            :ui {:label {:en "Corner"
                                                         :de "Kanten"}}}
                                   :thickness-1 {:type :range
                                                 :min 1
                                                 :max 10
                                                 :default 6
                                                 :ui {:label strings/thickness
                                                      :step 0.01}}
                                   :tincture-1 {:type :choice
                                                :choices tincture/choices
                                                :default :none
                                                :ui {:label strings/tincture
                                                     :form-type :tincture-select}}
                                   )
          (= mode :double) (assoc :thickness-2 {:type :range
                                                :min 1
                                                :max 10
                                                :default 3
                                                :ui {:label (util/str-tr strings/thickness " 2")
                                                     :step 0.01}}
                                  :tincture-2 {:type :choice
                                               :choices tincture/choices
                                               :default :none
                                               :ui {:label (util/str-tr strings/tincture " 2")
                                                    :form-type :tincture-select}}))
        (options/populate-inheritance inherited))))

(defn linejoin [corner]
  (case corner
    :round "round"
    :sharp "miter"
    :bevel "bevel"
    "round"))

(defn dilate-and-fill-path [shape negate-shape thickness color {:keys [svg-export?]}
                            & {:keys [fill? corner] :or {fill? true}}]
  (let [mask-id (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d shape
               :fill (when fill? "#ffffff")
               :style {:stroke-width thickness
                       :stroke "#ffffff"
                       :stroke-linejoin linejoin-value
                       :stroke-miterlimit 10}}]
       (when negate-shape
         [:path {:d negate-shape
                 :fill "#000000"
                 :style {:stroke-width thickness
                         :stroke "#ffffff"
                         :stroke-linejoin linejoin-value
                         :stroke-miterlimit 10}}])]]

     [:rect {:x -500
             :y -500
             :width 1100
             :height 1100
             :mask (str "url(#" mask-id ")")
             :fill color
             :style (when (not svg-export?)
                      {:pointer-events "none"})}]]))

(defn dilate-recursively [data stroke-width color linejoin]
  (walk/postwalk #(cond
                    (and (vector? %)
                         (-> % first (= :stroke-width))) [(first %) stroke-width]
                    (and (vector? %)
                         (-> % first (= :style))) [(first %) (-> %
                                                                 second
                                                                 (conj [:stroke-width stroke-width]))]
                    (and (vector? %)
                         (-> % first (= :stroke-linejoin))) [(first %) linejoin]
                    (and (vector? %)
                         (-> % first #{:stroke :fill :stop-color})
                         (-> % second (not= "none"))) [(first %) color]
                    :else %)
                 data))

(defn dilate-and-fill [shape thickness color {:keys [svg-export?]}
                       & {:keys [transform corner]}]
  (let [mask-id (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:g {:fill "#ffffff"
            :style {:stroke-width thickness
                    :stroke "#ffffff"
                    :stroke-linejoin linejoin-value
                    :stroke-miterlimit 10}}
        (dilate-recursively shape thickness "#ffffff" linejoin-value)]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [:g {:transform transform}
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill color
               :style (when (not svg-export?)
                        {:pointer-events "none"})}]]]]))

(defn render [line-path mask-id thickness color outline? corner context]
  [:g {:mask (when mask-id
               (str "url(#" mask-id ")"))}
   (when outline?
     [dilate-and-fill-path
      line-path
      nil
      (+ thickness
         outline/stroke-width)
      (outline/color context)
      context
      :corner corner
      :fill? false])
   (let [effective-thickness (cond-> thickness
                               outline? (- outline/stroke-width))]
     (when (> effective-thickness 0)
       [dilate-and-fill-path
        line-path
        nil
        effective-thickness
        color
        context
        :corner corner
        :fill? false]))])

(defn blazon-fimbriation [{:keys [tincture-1 tincture-2]}]
  (if tincture-2
    (util/str-tr strings/fimbriated " " (tincture/translate-tincture tincture-2)
                 " " strings/and " " (tincture/translate-tincture tincture-1))
    (when tincture-1
      (util/str-tr strings/fimbriated " " (tincture/translate-tincture tincture-1)))))

(defn blazon [context & {:keys [include-lines?]}]
  (->> (concat
        [(blazon-fimbriation (interface/get-sanitized-data (c/++ context :fimbriation)))]
        (when include-lines?
          [(blazon-fimbriation (interface/get-sanitized-data (c/++ context :line :fimbriation)))
           (blazon-fimbriation (interface/get-sanitized-data (c/++ context :opposite-line :fimbriation)))
           (blazon-fimbriation (interface/get-sanitized-data (c/++ context :extra-line :fimbriation)))]))
       distinct
       (util/combine ", ")))

(ns heraldry.coat-of-arms.line.fimbriation
  (:require [clojure.walk :as walk]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(def type-choices
  [["None" :none]
   ["Single" :single]
   ["Double" :double]])

(def type-map
  (util/choices->map type-choices))

(def alignment-choices
  [["Even" :even]
   ["Outside" :outside]
   ["Inside" :inside]])

(def alignment-map
  (util/choices->map alignment-choices))

(def default-options
  {:mode {:type :choice
          :choices type-choices
          :default :none
          :ui {:form-type :radio-select}}
   :alignment {:type :choice
               :choices alignment-choices
               :default :even
               :ui {:label "Alignment"}}
   :corner {:type :choice
            :choices [["Round" :round]
                      ["Sharp" :sharp]
                      ["Bevel" :bevel]]
            :default :sharp
            :ui {:label "Corner"}}
   :thickness-1 {:type :range
                 :min 1
                 :max 10
                 :default 6
                 :ui {:label "Thickness"
                      :step 0.01}}
   :tincture-1 {:type :choice
                :choices tincture/choices
                :default :none
                :ui {:label "Tincture"
                     :step 0.01
                     :form-type :tincture-select}}
   :thickness-2 {:type :range
                 :min 1
                 :max 10
                 :default 3
                 :ui {:label "Thickness 2"
                      :step 0.01}}
   :tincture-2 {:type :choice
                :choices tincture/choices
                :default :none
                :ui {:label "Tincture 2"
                     :step 0.01
                     :form-type :tincture-select}}
   :ui {:label "Fimbriation"
        :form-type :fimbriation}})

(defn options [fimbriation & {:keys [inherited base-options]
                              :or {base-options default-options}}]
  (-> (case (or (:mode fimbriation)
                (:mode inherited)
                :none)
        :none (options/pick base-options
                            [[:mode]])
        :single (options/pick base-options
                              [[:mode]
                               [:alignment]
                               [:corner]
                               [:thickness-1]
                               [:tincture-1]])
        :double (options/pick base-options
                              [[:mode]
                               [:alignment]
                               [:corner]
                               [:thickness-1]
                               [:tincture-1]
                               [:thickness-2]
                               [:tincture-2]]))
      (options/populate-inheritance inherited)))

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
      outline/color
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

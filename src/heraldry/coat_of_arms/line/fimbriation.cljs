(ns heraldry.coat-of-arms.line.fimbriation
  (:require [clojure.walk :as walk]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.util :as util]))

(defn linejoin [corner]
  (case corner
    :round "round"
    :sharp "miter"
    :bevel "bevel"
    "round"))

(defn dilate-and-fill-path [shape negate-shape thickness color {:keys [svg-export?]}
                            & {:keys [fill? corner] :or {fill? true}}]
  (let [mask-id        (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d     shape
               :fill  (when fill? "#ffffff")
               :style {:stroke-width      thickness
                       :stroke            "#ffffff"
                       :stroke-linejoin   linejoin-value
                       :stroke-miterlimit 10}}]
       (when negate-shape
         [:path {:d     negate-shape
                 :fill  "#000000"
                 :style {:stroke-width      thickness
                         :stroke            "#ffffff"
                         :stroke-linejoin   linejoin-value
                         :stroke-miterlimit 10}}])]]

     [:rect {:x      -500
             :y      -500
             :width  1100
             :height 1100
             :mask   (str "url(#" mask-id ")")
             :fill   color
             :style  (when (not svg-export?)
                       {:pointer-events "none"})}]]))

(defn dilate-recursively [data stroke-width color linejoin]
  (walk/postwalk #(cond
                    (and (vector? %)
                         (-> % first (= :stroke-width)))    [(first %) stroke-width]
                    (and (vector? %)
                         (-> % first (= :style)))           [(first %) (-> %
                                                                           second
                                                                           (conj [:stroke-width stroke-width]))]
                    (and (vector? %)
                         (-> % first (= :stroke-linejoin))) [(first %) linejoin]
                    (and (vector? %)
                         (-> % first #{:stroke :fill :stop-color})
                         (-> % second (not= "none")))       [(first %) color]
                    :else                                   %)
                 data))

(defn dilate-and-fill [shape thickness color {:keys [svg-export?]}
                       & {:keys [transform corner]}]
  (let [mask-id        (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:g {:fill  "#ffffff"
            :style {:stroke-width      thickness
                    :stroke            "#ffffff"
                    :stroke-linejoin   linejoin-value
                    :stroke-miterlimit 10}}
        (dilate-recursively shape thickness "#ffffff" linejoin-value)]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [:g {:transform transform}
       [:rect {:x      -500
               :y      -500
               :width  1100
               :height 1100
               :fill   color
               :style  (when (not svg-export?)
                         {:pointer-events "none"})}]]]]))

(defn render [line-path mask-id thickness color outline? corner render-options]
  [:g {:mask (when mask-id
               (str "url(#" mask-id ")"))}
   (when outline?
     [dilate-and-fill-path
      line-path
      nil
      (+ thickness
         outline/stroke-width)
      outline/color
      render-options
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
        render-options
        :corner corner
        :fill? false]))])

(ns heraldicon.heraldry.achievement
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]))

(defn- transform-bounding-box [^BoundingBox {:keys [min-x min-y]
                                             :as bounding-box}
                               ^js/Number target-width & {:keys [^js/Number max-aspect-ratio]}]
  (let [[total-width total-height] (bb/size bounding-box)
        target-height (-> target-width
                          (/ total-width)
                          (* total-height))
        target-height (if max-aspect-ratio
                        (min target-height
                             (* max-aspect-ratio target-width))
                        target-height)
        scale (min (/ target-width total-width)
                   (/ target-height total-height))]
    {:result-width target-width
     :result-height target-height
     :result-transform (str
                        "translate(" (/ target-width 2) "," (/ target-height 2) ")"
                        "scale(" scale "," scale ")"
                        "translate("
                        (- 0 (/ total-width 2) min-x) ","
                        (- 0 (/ total-height 2) min-y) ")")}))

(defn- get-used-fonts [context]
  (let [num-ornaments (interface/get-list-size (c/++ context :ornaments :elements))]
    ;; TODO: might have to be smarter here to only look into mottos,
    ;; but it should work if there's no :ribbon :segments
    (into #{}
          (filter identity)
          (for [i (range num-ornaments)
                j (range (interface/get-list-size (update context :path
                                                          conj
                                                          :ornaments
                                                          :elements
                                                          i
                                                          :ribbon
                                                          :segments)))]
            (interface/get-sanitized-data (update context :path
                                                  conj
                                                  :ornaments
                                                  :elements
                                                  i
                                                  :ribbon
                                                  :segments
                                                  j
                                                  :font))))))

(defmethod interface/render-component :heraldry/achievement [context]
  (let [{:keys [short-url
                svg-export?
                target-width
                target-height
                embed-fonts]} (c/render-hints context)
        escutcheon-shadow? (when-not svg-export?
                             (interface/render-option :escutcheon-shadow? context))
        short-url-font :deja-vu-sans
        scope (interface/render-option :scope context)
        coat-of-arms-angle (if (= scope :coat-of-arms)
                             0
                             (interface/render-option :coat-of-arms-angle context))
        coa-angle-rad-abs (-> coat-of-arms-angle
                              Math/abs
                              (* Math/PI)
                              (/ 180))
        coa-angle-counter-rad-abs (- (/ Math/PI 2)
                                     coa-angle-rad-abs)
        coat-of-arms-context (c/++ context :coat-of-arms)
        coat-of-arms-bounding-box (interface/get-bounding-box coat-of-arms-context)
        [coat-of-arms-width coat-of-arms-height] (bb/size coat-of-arms-bounding-box)
        render-helms? (#{:achievement :coat-of-arms-and-helm} scope)
        helms-context (-> (c/++ context :helms)
                          (c/set-key :parent-environment-override (environment/create coat-of-arms-bounding-box)))
        helms-bounding-box (when render-helms?
                             (interface/get-bounding-box helms-context))
        short-arm (* coat-of-arms-width (Math/cos coa-angle-rad-abs))
        long-arm (* coat-of-arms-height (Math/cos coa-angle-counter-rad-abs))
        [rotated-min-x
         rotated-max-x] (if (neg? coat-of-arms-angle)
                          [(- long-arm)
                           short-arm]
                          [(- coat-of-arms-width short-arm)
                           (+ coat-of-arms-width long-arm)])
        rotated-height (+ (* coat-of-arms-width (Math/sin coa-angle-rad-abs))
                          (* coat-of-arms-height (Math/sin coa-angle-counter-rad-abs)))
        rotated? (not (zero? coat-of-arms-angle))
        helm-position (cond
                        (neg? coat-of-arms-angle) (v/Vector. (- (/ coat-of-arms-width 2)) 0)
                        (pos? coat-of-arms-angle) (v/Vector. (/ coat-of-arms-width 2) 0)
                        :else v/zero)
        helms-bounding-box (when helms-bounding-box
                             (bb/translate helms-bounding-box helm-position))
        coat-of-arms-bounding-box (if rotated?
                                    (bb/BoundingBox. rotated-min-x rotated-max-x
                                                     0 rotated-height)
                                    (bb/BoundingBox. 0 coat-of-arms-width
                                                     0 coat-of-arms-height))
        coa-and-helms-bounding-box (bb/combine coat-of-arms-bounding-box helms-bounding-box)

        ornaments-context (-> (c/++ context :ornaments)
                              (c/set-key :parent-environment-override (environment/create coat-of-arms-bounding-box)))
        render-ornaments? (= scope :achievement)
        ornaments-bounding-box (when render-ornaments?
                                 (interface/get-bounding-box ornaments-context))

        used-fonts (cond-> (get-used-fonts context)
                     short-url (conj short-url-font))

        achievement-bounding-box (bb/combine ornaments-bounding-box coa-and-helms-bounding-box)

        result-width 1000
        {:keys [result-width
                result-height
                result-transform]} (transform-bounding-box
                                    achievement-bounding-box
                                    result-width
                                    :max-aspect-ratio 1.5)
        margin 10
        font-size 20
        result-width (+ result-width (* 2 margin))
        result-height (-> (+ result-height (* 2 margin))
                          (cond->
                            escutcheon-shadow? (+ 10)
                            short-url (+ font-size margin)))

        scale (if (and svg-export?
                       (or target-width
                           target-height))
                (let [scale-width (when target-width
                                    (/ target-width result-width))
                      scale-height (when target-height
                                     (/ target-height result-height))]
                  (or (when (and scale-width scale-height)
                        (min scale-width scale-height))
                      scale-width
                      scale-height))
                1)
        [document-width document-height] [(* result-width scale) (* result-height scale)]]
    [:svg (merge
           {:viewBox (str/join " " (map str [(- margin) (- margin)
                                             result-width result-height]))}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :xmlnsXlink "http://www.w3.org/1999/xlink"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when (and svg-export?
                embed-fonts)
       [embed-fonts used-fonts])
     [:g {:transform result-transform
          :style {:transition "transform 0.5s"}}

      (when render-ornaments?
        [interface/render-component (c/set-render-hint ornaments-context :render-pass-below-shield? true)])

      (when render-helms?
        [:g {:transform (str "translate(" (v/->str helm-position) ")")
             :style {:transition "transform 0.5s"}}
         [interface/render-component (c/set-render-hint helms-context :render-pass-below-shield? true)]])

      [:g {:transform (cond
                        (neg? coat-of-arms-angle) (str "rotate(" (- coat-of-arms-angle) ")")
                        (pos? coat-of-arms-angle) (str "translate(" coat-of-arms-width "," 0 ")"
                                                       "rotate(" (- coat-of-arms-angle) ")"
                                                       "translate(" (- coat-of-arms-width) "," 0 ")")
                        :else nil)
           :style {:transition "transform 0.5s"}}
       [interface/render-component coat-of-arms-context]]

      (when render-helms?
        [:g {:transform (str "translate(" (v/->str helm-position) ")")
             :style {:transition "transform 0.5s"}}
         [interface/render-component (c/set-render-hint helms-context :render-pass-below-shield? false)]])

      (when render-ornaments?
        [interface/render-component (c/set-render-hint ornaments-context :render-pass-below-shield? false)])]

     (when short-url
       [:text {:x 0
               :y (- result-height margin (/ font-size 2))
               :text-anchor "start"
               :fill "#888"
               :style {:font-family (font/css-string short-url-font)
                       :font-size font-size}}
        short-url])]))

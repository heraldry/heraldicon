(ns heraldry.coat-of-arms.hatching)

(def spacing 2.5)
(def line-thickness 0.2)

(def argent
  (let [id "hatching-argent"]
    [id
     (let [width 10]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]])]))

#_{:clj-kondo/ignore [:redefined-var]}
(def or
  (let [id "hatching-or"]
    [id
     (let [width (* spacing 2)
           size 0.5]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:fill "#000"}
         [:circle {:cx 0
                   :cy 0
                   :r size}]
         [:circle {:cx width
                   :cy 0
                   :r size}]
         [:circle {:cx 0
                   :cy width
                   :r size}]
         [:circle {:cx width
                   :cy width
                   :r size}]
         [:circle {:cx spacing
                   :cy spacing
                   :r size}]]])]))

(def azure
  (let [id "hatching-azure"]
    [id
     [:pattern {:id id
                :width spacing
                :height spacing
                :pattern-units "userSpaceOnUse"}
      [:rect {:x 0
              :y 0
              :width spacing
              :height spacing
              :fill "#f5f5f5"}]
      [:g {:stroke "#000"
           :stroke-width line-thickness}
       [:path {:d (str "M" 0 "," 0 " L" spacing "," 0)}]
       [:path {:d (str "M" 0 "," spacing " L" spacing "," spacing)}]]]]))

(def vert
  (let [id "hatching-vert"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (- width) "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" 0 "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (- width) "," 0 " l" (* width 3) "," (* width 3))}]]])]))

(def gules
  (let [id "hatching-gules"]
    [id
     [:pattern {:id id
                :width spacing
                :height spacing
                :pattern-units "userSpaceOnUse"}
      [:rect {:x 0
              :y 0
              :width spacing
              :height spacing
              :fill "#f5f5f5"}]
      [:g {:stroke "#000"
           :stroke-width line-thickness}
       [:path {:d (str "M" 0 "," 0 " L" 0 "," spacing)}]
       [:path {:d (str "M" spacing "," 0 " L" spacing "," spacing)}]]]]))

(def purpure
  (let [id "hatching-purpure"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (* width 2) "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" width "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," 0 " l" (* width -3) "," (* width 3))}]]])]))

(def sable
  (let [id "hatching-sable"]
    [id
     [:pattern {:id id
                :width spacing
                :height spacing
                :pattern-units "userSpaceOnUse"}
      [:rect {:x 0
              :y 0
              :width spacing
              :height spacing
              :fill "#f5f5f5"}]
      [:g {:stroke "#000"
           :stroke-width line-thickness}
       [:path {:d (str "M" 0 "," 0 " L" spacing "," 0)}]
       [:path {:d (str "M" 0 "," spacing " L" spacing "," spacing)}]
       [:path {:d (str "M" 0 "," 0 " L" 0 "," spacing)}]
       [:path {:d (str "M" spacing "," 0 " L" spacing "," spacing)}]]]]))

(def murrey
  (let [id "hatching-murrey"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (- width) "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" 0 "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (- width) "," 0 " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" width "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," 0 " l" (* width -3) "," (* width 3))}]]])]))

(def sanguine
  (let [id "hatching-sanguine"]
    [id
     [:<>
      [:pattern {:id "hatching-sanguine-secondary"
                 :width spacing
                 :height spacing
                 :pattern-units "userSpaceOnUse"}
       [:g {:stroke "#000"
            :stroke-width line-thickness}
        [:path {:d (str "M" 0 "," 0 " L" spacing "," 0)}]
        [:path {:d (str "M" 0 "," spacing " L" spacing "," spacing)}]]]
      (let [width 150
            height 200]
        [:pattern {:id id
                   :width width
                   :height height
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x 0
                 :y 0
                 :width width
                 :height height
                 :fill "#f5f5f5"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-vert)"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-sanguine-secondary)"}]])]]))

(def tenne
  (let [id "hatching-tenne"]
    [id
     [:<>
      [:pattern {:id "hatching-tenne-secondary"
                 :width spacing
                 :height spacing
                 :pattern-units "userSpaceOnUse"}
       [:g {:stroke "#000"
            :stroke-width line-thickness}
        [:path {:d (str "M" 0 "," 0 " L" 0 "," spacing)}]
        [:path {:d (str "M" spacing "," 0 " L" spacing "," spacing)}]]]
      (let [width 150
            height 200]
        [:pattern {:id id
                   :width width
                   :height height
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x 0
                 :y 0
                 :width width
                 :height height
                 :fill "#f5f5f5"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-vert)"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-tenne-secondary)"}]])]]))

(def metals
  {:argent argent
   :or or})

(def colours
  {;; tincture
   :azure azure
   :vert vert
   :gules gules
   :sable sable
   :purpure purpure
   ;; stain
   :murrey murrey
   :sanguine sanguine
   :tenne tenne
   ;; nontraditional
   :white argent})

(def patterns
  (into
   [:<>]
   (for [[_ pattern] (vals (concat metals colours))]
     pattern)))

(defn get-for [tincture]
  (let [[id _] (clojure.core/or (get metals tincture)
                                (get colours tincture))]
    (when id
      (str "url(#" id ")"))))

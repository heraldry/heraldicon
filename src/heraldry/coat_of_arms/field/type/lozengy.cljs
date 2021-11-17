(ns heraldry.coat-of-arms.field.type.lozengy
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/lozengy)

(defmethod field-interface/display-name field-type [_] {:en "Lozengy"
                                                        :de "Schräggewürfelt"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod interface/options field-type [_context]
  {:layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label strings/subfields-x
                                :form-type :field-layout-num-fields-x}}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label strings/subfields-y
                                :form-type :field-layout-num-fields-y}}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label strings/offset-x
                            :step 0.01}}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label strings/offset-y
                            :step 0.01}}
            :stretch-x {:type :range
                        :min 0.2
                        :max 3
                        :default 1
                        :ui {:label strings/stretch-x
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.2
                        :max 3
                        :default 1
                        :ui {:label strings/stretch-y
                             :step 0.01}}
            :rotation {:type :range
                       :min -90
                       :max 90
                       :default 0
                       :ui {:label strings/rotation
                            :step 0.01}}
            :ui {:label strings/layout
                 :form-type :field-layout}}})

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        unstretched-part-width (-> width
                                   (/ num-fields-x))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "lozengy")
        lozenge-shape (path/make-path ["M" [(/ part-width 2) 0]
                                       "L" [part-width (/ part-height 2)]
                                       "L" [(/ part-width 2) part-height]
                                       "L" [0 (/ part-height 2)]
                                       "z"])]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          [:path {:d lozenge-shape}]]])
      [:pattern {:id (str pattern-id "-0")
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :fill "#000000"}]
       [:path {:d lozenge-shape
               :fill "#ffffff"}]]
      [:pattern {:id (str pattern-id "-1")
                 :width part-width
                 :height part-height
                 :x (+ (:x top-left)
                       (* part-width offset-x)
                       shift-x)
                 :y (+ (:y top-left)
                       (* part-height offset-y)
                       shift-y)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :fill "#ffffff"}]
       [:path {:d lozenge-shape
               :fill "#000000"}]]]
     [:g {:transform (str "rotate(" (- rotation) ")")}
      (doall
       (for [idx (range 2)]
         (let [mask-id (util/id "mask")]
           ^{:key idx}
           [:<>
            [:mask {:id mask-id}
             [:rect {:x -500
                     :y -500
                     :width 1100
                     :height 1100
                     :fill (str "url(#" pattern-id "-" idx ")")}]]
            [tincture/tinctured-field
             (c/++ context :fields idx :tincture)
             :mask-id mask-id
             :transform (str "rotate(" rotation ")")]])))
      (when outline?
        [:rect {:x -500
                :y -500
                :width 1100
                :height 1100
                :fill (str "url(#" pattern-id "-outline)")}])]]))

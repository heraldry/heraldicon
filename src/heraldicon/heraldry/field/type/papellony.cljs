(ns heraldicon.heraldry.field.type.papellony
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/papellony)

(defmethod field.interface/display-name field-type [_] :string.field.type/papellony)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:thickness {:type :range
               :min 0
               :max 0.5
               :default 0.1
               :ui {:label :string.option/thickness
                    :step 0.01}}
   :layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label :string.option/subfields-x
                                :form-type :field-layout-num-fields-x}}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label :string.option/subfields-y
                                :form-type :field-layout-num-fields-y}}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label :string.option/offset-x
                            :step 0.01}}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label :string.option/offset-y
                            :step 0.01}}
            :stretch-x {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label :string.option/stretch-x
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label :string.option/stretch-y
                             :step 0.01}}
            :ui {:label :string.option/layout
                 :form-type :field-layout}}})

(defn papellony-default [part-width part-height thickness]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        thickness (* thickness width)
        extra (-> (- 1 (/ (* thickness thickness)
                          (* middle-x middle-x)))
                  Math/sqrt
                  (* middle-y)
                  (->> (- middle-y)))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0,0"
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M" (- middle-x) "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M" middle-x "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0"
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- extra)
                               "h" thickness
                               "v" extra
                               "z")}]
               [:path {:d (str "M" width "," height
                               "v" (- extra)
                               "h" (- thickness)
                               "v" extra
                               "z")}]]

     :outline [:<>
               [:path {:d (str "M 0,0"
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- width thickness) ",0"
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" (- middle-x) "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- middle-x thickness) "," middle-y
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" middle-x "," middle-y
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (+ middle-x
                                      width
                                      (- thickness)) "," middle-y
                               "a" (- middle-x thickness) " " (- middle-y thickness) " 0 0 1 "
                               (- (- width
                                     (* 2 thickness))) " 0")}]
               [:path {:d (str "M" (- middle-x) "," (- middle-y)
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" middle-x "," (- middle-y)
                               "a" middle-x " " middle-y " 0 0 0 " width " 0")}]
               [:path {:d (str "M" (- middle-x thickness) "," middle-y
                               "v" (- extra))}]
               [:path {:d (str "M" (+ middle-x thickness) "," middle-y
                               "v" (- extra))}]
               [:path {:d (str "M" thickness "," height
                               "v" (- extra))}]
               [:path {:d (str "M" (- width thickness) "," height
                               "v" (- extra))}]]}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
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
                                  (/ part-width 2))
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id-prefix (uid/generate "papellony")
        {pattern-width :width
         pattern-height :height
         papellony-pattern :pattern
         papellony-outline :outline} (papellony-default part-width part-height thickness)]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id-prefix "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          papellony-outline]])
      (for [idx (range 2)]
        ^{:key idx}
        [:pattern {:id (str pattern-id-prefix "-" idx)
                   :width pattern-width
                   :height pattern-height
                   :x (+ (:x top-left)
                         (* part-width offset-x)
                         shift-x)
                   :y (+ (:y top-left)
                         (* part-height offset-y)
                         shift-y)
                   :pattern-units "userSpaceOnUse"}
         [:rect {:x 0
                 :y 0
                 :width pattern-width
                 :height pattern-height
                 :fill (get ["#ffffff" "#000000"] idx)}]
         [:g {:fill (get ["#000000" "#ffffff"] idx)}
          papellony-pattern]])]
     (doall
      (for [idx (range 2)]
        (let [mask-id (uid/generate "mask")]
          ^{:key idx}
          [:<>
           [:mask {:id mask-id}
            [:rect {:x -500
                    :y -500
                    :width 1100
                    :height 1100
                    :fill (str "url(#" pattern-id-prefix "-" idx ")")}]]
           [tincture/tinctured-field
            (c/++ context :fields idx)
            :mask-id mask-id]])))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id-prefix "-outline)")}])]))

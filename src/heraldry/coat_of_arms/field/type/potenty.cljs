(ns heraldry.coat-of-arms.field.type.potenty
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/potenty)

(defmethod field-interface/display-name field-type [_] {:en "Potenty"
                                                        :de "Krückenfeh"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod interface/options-subscriptions field-type [_context]
  #{})

(defmethod interface/options field-type [_context]
  {:variant {:type :choice
             :choices [["Default" :default]
                       ["Counter" :counter]
                       ["In pale" :in-pale]
                       ["En point" :en-point]]
             :default :default
             :ui {:label strings/variant}}
   :layout {:num-fields-x {:type :range
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
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label strings/stretch-x
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label strings/stretch-y
                             :step 0.01}}
            :ui {:label strings/layout
                 :form-type :field-layout}}})

(defn units [n]
  (-> n
      (* 4)
      (- 1)))

(defn potent-default [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M " middle-x "," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "H" width
                               "V" height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn potent-counter [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn potent-in-pale [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]
               [:path {:d (str "M" width "," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" width "," height
                               "v" (- h))}]]}))

(defn potent-en-point [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" (* 2 h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" h)}]
               [:path {:d (str "M " width "," height
                               "v" (- h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "v" (- h))}]
               [:path {:d (str "M 0,0"
                               "h" w)}]
               [:path {:d (str "M" width ",0"
                               "h" w)}]
               [:path {:d (str "M 0," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" middle-x "," height
                               "h" w)}]]}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [variant (interface/get-sanitized-data (c/++ context :variant))
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
                                   (/ (units num-fields-x))
                                   (* 4))
        part-width (-> unstretched-part-width
                       (* stretch-x))
        height (- (:y bottom-right)
                  (:y top-left))
        unstretched-part-height (if raw-num-fields-y
                                  (-> height
                                      (/ num-fields-y))
                                  (-> part-width
                                      (/ 2)))
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "potenty")
        potent-function (case variant
                          :counter potent-counter
                          :in-pale potent-in-pale
                          :en-point potent-en-point
                          potent-default)
        {pattern-width :width
         pattern-height :height
         potent-pattern :pattern
         potent-outline :outline} (potent-function part-width part-height)]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
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
          potent-outline]])
      (doall
       (for [idx (range 2)]
         ^{:key idx}
         [:pattern {:id (str pattern-id "-" idx)
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
                  :fill (get ["#000000" "#ffffff"] idx)}]
          [:g {:fill (get ["#ffffff" "#000000"] idx)}
           potent-pattern]]))]
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
            :mask-id mask-id]])))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id "-outline)")}])]))

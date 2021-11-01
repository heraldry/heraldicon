(ns heraldry.coat-of-arms.field.type.chequy
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(def field-type :heraldry.field.type/chequy)

(defmethod field-interface/display-name field-type [_] {:en "Chequy"
                                                        :de "Geschacht"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [num-base-fields (interface/get-sanitized-data (c/++ context :layout :num-base-fields))
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
                                  part-width)
        part-height (-> unstretched-part-height
                        (* stretch-y))
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        pattern-id (util/id "chequy")]
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
          [:path {:d (str "M 0,0 h " part-width)}]
          [:path {:d (str "M 0,0 v " part-height)}]
          [:path {:d (str "M 0," part-height " h " part-width)}]
          [:path {:d (str "M " part-width ",0 v " part-height)}]]])
      (doall
       (for [idx (range num-base-fields)]
         ^{:key idx}
         [:pattern {:id (str pattern-id "-" idx)
                    :width (* part-width num-base-fields)
                    :height (* part-height num-base-fields)
                    :x (+ (:x top-left)
                          (* part-width offset-x)
                          shift-x)
                    :y (+ (:y top-left)
                          (* part-height offset-y)
                          shift-y)
                    :pattern-units "userSpaceOnUse"}
          [:rect {:x 0
                  :y 0
                  :width (* part-width num-base-fields)
                  :height (* part-height num-base-fields)
                  :fill "#000000"}]
          (for [j (range num-base-fields)
                i (range num-base-fields)]
            (when (-> i (+ j) (mod num-base-fields) (= idx))
              ^{:key [i j]}
              [:rect {:x (* i part-width)
                      :y (* j part-height)
                      :width part-width
                      :height part-height
                      :fill "#ffffff"}]))]))]
     (doall
      (for [idx (range num-base-fields)]
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

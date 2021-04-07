(ns heraldry.coat-of-arms.field.type.tierced-per-fess
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per fess"
   :value        :tierced-per-fess
   :parts        ["chief" "fess" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin]}      (options/sanitize division (field-options/options division))
        {:keys [stretch-y]}               layout
        points                            (:points environment)
        origin-point                      (position/calculate origin environment :fess)
        top-left                          (:top-left points)
        bottom-right                      (:bottom-right points)
        left                              (assoc (:left points) :y (:y origin-point))
        right                             (assoc (:right points) :y (:y origin-point))
        height                            (:height environment)
        middle-half-height                (-> height
                                              (/ 6)
                                              (* stretch-y))
        row1                              (- (:y origin-point) middle-half-height)
        row2                              (+ (:y origin-point) middle-half-height)
        [first-left first-right]          (v/environment-intersections
                                           (v/v (:x left) row1)
                                           (v/v (:x right) row1)
                                           environment)
        [second-left second-right]        (v/environment-intersections
                                           (v/v (:x left) row2)
                                           (v/v (:x right) row2)
                                           environment)
        shared-start-x                    (- (min (:x first-left)
                                                  (:x second-left))
                                             30)
        real-start                        (min (-> first-left :x (- shared-start-x))
                                               (-> second-left :x (- shared-start-x)))
        real-end                          (max (-> first-right :x (- shared-start-x))
                                               (-> second-right :x (- shared-start-x)))
        shared-end-x                      (+ real-end 30)
        first-left                        (v/v shared-start-x (:y first-left))
        second-left                       (v/v shared-start-x (:y second-left))
        first-right                       (v/v shared-end-x (:y first-right))
        second-right                      (v/v shared-end-x (:y second-right))
        {line-one       :line
         line-one-start :line-start}      (line/create line
                                                        first-left first-right
                                                        :real-start real-start
                                                        :real-end real-end
                                                        :render-options render-options
                                                        :environment environment)
        {line-reversed       :line
         line-reversed-start :line-start} (line/create line
                                                        second-left second-right
                                                        :reversed? true
                                                        :flipped? true
                                                        :real-start real-start
                                                        :real-end real-end
                                                        :render-options render-options
                                                        :environment environment)
        parts                             [[["M" (v/+ first-left
                                                      line-one-start)
                                             (svg/stitch line-one)
                                             (infinity/path :counter-clockwise
                                                            [:right :left]
                                                            [(v/+ first-right
                                                                  line-one-start)
                                                             (v/+ first-left
                                                                  line-one-start)])
                                             "z"]
                                            [top-left first-right]]

                                           [["M" (v/+ first-left
                                                      line-one-start)
                                             (svg/stitch line-one)
                                             (infinity/path :clockwise
                                                            [:right :right]
                                                            [(v/+ first-left
                                                                  line-one-start)
                                                             (v/+ second-right
                                                                  line-reversed-start)])
                                             (svg/stitch line-reversed)
                                             (infinity/path :clockwise
                                                            [:left :left]
                                                            [(v/+ second-left
                                                                  line-reversed-start)
                                                             (v/+ first-left
                                                                  line-one-start)])
                                             "z"]
                                            [first-left
                                             second-right]]

                                           [["M" (v/+ second-right
                                                      line-reversed-start)
                                             (svg/stitch line-reversed)
                                             (infinity/path :counter-clockwise
                                                            [:left :right]
                                                            [(v/+ second-left
                                                                  line-reversed-start)
                                                             (v/+ second-right
                                                                  line-reversed-start)])
                                             "z"]
                                            [second-left bottom-right]]]]
    [:<>
     [shared/make-division
      (shared/field-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ second-right
                   line-reversed-start)
          (svg/stitch line-reversed)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-left
                              line-one-start)
                     (svg/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-right
                              line-reversed-start)
                     (svg/stitch line-reversed)])}]])]))


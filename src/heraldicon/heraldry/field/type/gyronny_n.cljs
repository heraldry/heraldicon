(ns heraldicon.heraldry.field.type.gyronny-n
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/gyronny-n)

(defmethod field.interface/display-name field-type [_] :string.field.type/gyronny-n)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))]
    {:anchor {:point {:type :choice
                      :choices [[:string.option.point-choice/chief :chief]
                                [:string.option.point-choice/base :base]
                                [:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/dexter :dexter]
                                [:string.option.point-choice/sinister :sinister]
                                [:string.option.point-choice/honour :honour]
                                [:string.option.point-choice/nombril :nombril]
                                [:string.option.point-choice/center :center]
                                [:string.option.point-choice/angle :angle]]
                      :default :fess
                      :ui {:label :string.option/point}}
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :layout {:num-fields-x {:type :range
                             :min 3
                             :max 32
                             :default 6
                             :integer? true
                             :ui {:label :string.option/subfields
                                  :form-type :field-layout-num-fields-x}}
              :num-base-fields {:type :range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui {:label :string.option/base-fields
                                     :form-type :field-layout-num-base-fields}}
              :offset-x {:type :range
                         :min -1
                         :max 1
                         :default -0.5
                         :ui {:label :string.option/offset
                              :step 0.01}}
              :ui {:label :string.option/layout
                   :form-type :field-layout}}
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        num-fields (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        offset (interface/get-sanitized-data (c/++ context :layout :offset-x))
        anchor-point (position/calculate anchor environment)
        angle-step (/ 360 num-fields)
        start-angle (* offset angle-step)
        arm-angles (map #(-> %
                             (* angle-step)
                             (+ start-angle))
                        (range num-fields))
        arm-intersections (mapv (fn [angle]
                                  (v/find-first-intersection-of-ray
                                   anchor-point
                                   (v/add anchor-point
                                          (v/rotate (v/Vector. 0 -1) angle))
                                   environment))
                                arm-angles)
        arm-length (->> arm-intersections
                        (map #(-> %
                                  (v/sub anchor-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        line (dissoc line :fimbriation)
        arm-points (mapv (fn [angle]
                           (-> (v/Vector. 0 -1)
                               (v/rotate angle)
                               (v/mul full-arm-length)
                               (v/add anchor-point)))
                         arm-angles)
        fess-points (mapv (fn [angle]
                            (-> (v/find-first-intersection-of-ray
                                 anchor-point
                                 (v/add anchor-point
                                        (v/rotate (v/Vector. 0 -1)
                                                  (+ angle
                                                     (/ angle-step 2))))
                                 environment)
                                (v/sub anchor-point)
                                (v/mul 0.6)
                                (v/add anchor-point)))
                          arm-angles)
        infinity-points (vec (mapcat (fn [angle]
                                       [(-> (v/Vector. 0 -1)
                                            (v/rotate angle)
                                            (v/mul full-arm-length)
                                            (v/mul 2)
                                            (v/add anchor-point))
                                        (-> (v/Vector. 0 -1)
                                            (v/rotate (+ angle (/ angle-step 2)))
                                            (v/mul full-arm-length)
                                            (v/mul 2)
                                            (v/add anchor-point))])
                                     arm-angles))
        lines (mapv (fn [arm-point]
                      (line/create line
                                   anchor-point
                                   arm-point
                                   :flipped? true
                                   :mirrored? true
                                   :real-start 0
                                   :real-end arm-length
                                   :context context
                                   :environment environment))
                    arm-points)
        reverse-lines (mapv (fn [arm-point]
                              (line/create line
                                           anchor-point
                                           arm-point
                                           :reversed? true
                                           :real-start 0
                                           :real-end arm-length
                                           :context context
                                           :environment environment))
                            arm-points)
        parts (mapv (fn [index]
                      (let [next-index (-> index
                                           inc
                                           (mod num-fields))
                            [index-1 index-2] (if (even? index)
                                                [index next-index]
                                                [next-index index])
                            [arm-point-1
                             {line-1 :line
                              line-1-start :line-start}] [(nth arm-points index-1)
                                                          (nth reverse-lines index-1)]
                            [arm-point-2
                             {line-2 :line}] [(nth arm-points index-2)
                                              (nth lines index-2)]]
                        [["M" (v/add arm-point-1
                                     line-1-start)
                          (path/stitch line-1)
                          "L" anchor-point
                          (path/stitch line-2)
                          "L" (nth infinity-points (* 2 index-2))
                          "L" (nth infinity-points (-> index
                                                       (* 2)
                                                       inc))
                          "L" (nth infinity-points (* 2 index-1))
                          "z"]
                         [arm-point-1
                          anchor-point
                          arm-point-2]
                         {:points {:fess (nth fess-points index)}}]))
                    (range num-fields))
        arm-outlines (mapv (fn [index]
                             (let [next-index (-> index
                                                  inc
                                                  (mod num-fields))
                                   arm-line (nth lines next-index)]
                               (path/make-path
                                ["M" anchor-point
                                 (path/stitch (:line arm-line))])))
                           (range num-fields))]
    [:<>
     [shared/make-subfields
      context parts
      (conj (->> arm-outlines
                 (drop 1)
                 drop-last
                 (map vector)
                 (into [:all]))
            nil)
      environment]
     (when outline?
       (into [:g (outline/style context)]
             (map (fn [arm-line]
                    [:path {:d arm-line}]))
             arm-outlines))]))

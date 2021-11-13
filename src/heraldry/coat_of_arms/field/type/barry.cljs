(ns heraldry.coat-of-arms.field.type.barry
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.strings :as strings]))

(def field-type :heraldry.field.type/barry)

(defmethod field-interface/display-name field-type [_] {:en "Barry"
                                                        :de "Geteilt vielfach"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod interface/options field-type [context]
  (let [line-data (interface/get-raw-data (c/++ context :line))
        line-style (-> (line/options line-data)
                       (dissoc :fimbriation))]
    {:layout {:num-fields-y {:type :range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui {:label {:en "y-Subfields"
                                          :de "y-Unterfelder"}
                                  :form-type :field-layout-num-fields-y}}
              :num-base-fields {:type :range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui {:label {:en "Base fields"
                                             :de "Basisfelder"}
                                     :form-type :field-layout-num-base-fields}}
              :offset-y {:type :range
                         :min -1
                         :max 1
                         :default 0
                         :ui {:label strings/offset-y
                              :step 0.01}}
              :stretch-y {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label strings/stretch-y
                               :step 0.01}}
              :ui {:label strings/layout
                   :form-type :field-layout}}
     :line line-style}))

(defn barry-parts [top-left bottom-right line outline? context]
  (let [environment (:environment context)
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        height (- (:y bottom-right)
                  (:y top-left))
        bar-height (-> height
                       (/ num-fields-y)
                       (* stretch-y))
        required-height (* bar-height
                           num-fields-y)
        middle (-> height
                   (/ 2)
                   (+ (:y top-left)))
        y0 (-> middle
               (- (/ required-height 2))
               (+ (* offset-y
                     bar-height)))
        x1 (:x top-left)
        x2 (:x bottom-right)
        width (- x2 x1)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end} (line/create line
                                                top-left
                                                (v/add top-left (v/v width 0))
                                                :real-start 0
                                                :real-end width
                                                :context context
                                                :environment environment)
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end} (line/create line
                                               top-left
                                               (v/add top-left (v/v width 0))
                                               :flipped? true
                                               :mirrored? true
                                               :reversed? true
                                               :real-start 0
                                               :real-end width
                                               :context context
                                               :environment environment)
        parts (->> (range num-fields-y)
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                last-part? (-> i inc (= num-fields-y))
                                line-one-left (v/v x1 y1)
                                line-one-right (v/v x2 y1)
                                line-two-left (v/v x1 y2)
                                line-two-right (v/v x2 y2)]
                            [(cond
                               (and (zero? i)
                                    last-part?) ["M" -1000 -1000
                                                 "h" 2000
                                                 "v" 2000
                                                 "h" -2000
                                                 "z"]
                               (zero? i) ["M" (v/add line-two-left
                                                     line-right-start)
                                          (path/stitch line-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/add line-two-right
                                                                 line-right-end)
                                                          (v/add line-two-left
                                                                 line-right-start)])
                                          "z"]
                               (even? i) (concat ["M" (v/add line-one-right
                                                             line-left-start)
                                                  (path/stitch line-left)]
                                                 (cond
                                                   last-part? [(infinity/path :counter-clockwise
                                                                              [:left :right]
                                                                              [(v/add line-one-left
                                                                                      line-left-end)
                                                                               (v/add line-one-right
                                                                                      line-left-start)])
                                                               "z"]
                                                   :else [(infinity/path :counter-clockwise
                                                                         [:left :left]
                                                                         [(v/add line-one-left
                                                                                 line-left-end)
                                                                          (v/add line-two-left
                                                                                 line-right-start)])
                                                          (path/stitch line-right)
                                                          (infinity/path :counter-clockwise
                                                                         [:right :right]
                                                                         [(v/add line-two-right
                                                                                 line-right-end)
                                                                          (v/add line-one-right
                                                                                 line-left-start)])]))
                               :else (concat ["M" (v/add line-one-left
                                                         line-right-start)
                                              (path/stitch line-right)]
                                             (cond
                                               last-part? [(infinity/path :clockwise
                                                                          [:right :left]
                                                                          [(v/add line-one-right
                                                                                  line-right-end)
                                                                           (v/add line-one-left
                                                                                  line-right-start)])
                                                           "z"]
                                               :else [(infinity/path :clockwise
                                                                     [:right :right]
                                                                     [(v/add line-one-right
                                                                             line-right-end)
                                                                      (v/add line-two-right
                                                                             line-left-start)])
                                                      (path/stitch line-left)
                                                      (infinity/path :clockwise
                                                                     [:left :left]
                                                                     [(v/add line-two-left
                                                                             line-left-end)
                                                                      (v/add line-one-left
                                                                             line-right-start)])
                                                      "z"])))
                             [line-one-left line-two-right]])))
                   vec)
        edges (->> num-fields-y
                   dec
                   range
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                line-two-left (v/v x1 y2)
                                line-two-right (v/v x2 y2)]
                            (if (even? i)
                              (path/make-path ["M" (v/add line-two-left
                                                          line-right-start)
                                               (path/stitch line-right)])
                              (path/make-path ["M" (v/add line-two-right
                                                          line-left-start)
                                               (path/stitch line-left)])))))
                   vec)
        overlap (-> edges
                    (->> (map vector))
                    vec
                    (conj nil))
        outlines (when outline?
                   [:g (outline/style context)
                    (for [i (range (dec num-fields-y))]
                      ^{:key i}
                      [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [parts overlap outlines] (barry-parts top-left bottom-right line outline? context)]
    [:<>
     [shared/make-subfields
      context parts
      overlap
      environment]
     outlines]))

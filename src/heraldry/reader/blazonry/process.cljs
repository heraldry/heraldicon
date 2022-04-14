(ns heraldry.reader.blazonry.process
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.coat-of-arms.charge.options :as charge.options]))

(defn add-charge-group-defaults [{:heraldry.reader.blazonry.transform/keys [default-charge-group-amount]
                                  :keys [type]
                                  :as hdn} & {:keys [parent-ordinary-type]}]
  (let [type-namespace (some-> type namespace)]
    (cond-> hdn
      default-charge-group-amount (->
                                   (dissoc :heraldry.reader.blazonry.transform/default-charge-group-amount)
                                   (merge
                                    (case (some-> parent-ordinary-type name keyword)
                                      :pale {:type :heraldry.charge-group.type/columns
                                             :spacing (/ 95 default-charge-group-amount)
                                             :strips [{:type :heraldry.component/charge-group-strip
                                                       :slots (vec (repeat default-charge-group-amount 0))}]}

                                      :chevron {:type :heraldry.charge-group.type/rows
                                                :spacing (/ 90 default-charge-group-amount)
                                                :strips (->> (range (-> default-charge-group-amount
                                                                        inc
                                                                        (/ 2)))
                                                             (map (fn [index]
                                                                    {:type :heraldry.component/charge-group-strip
                                                                     :stretch (if (and (zero? index)
                                                                                       (even? default-charge-group-amount))
                                                                                1
                                                                                (if (and (pos? index)
                                                                                         (even? default-charge-group-amount))
                                                                                  (+ 1 (/ (inc index)
                                                                                          index))
                                                                                  2))
                                                                     :slots (if (zero? index)
                                                                              (if (odd? default-charge-group-amount)
                                                                                [0]
                                                                                [0 0])
                                                                              (-> (concat [0]
                                                                                          (repeat (dec index) nil)
                                                                                          [0])
                                                                                  vec))}))
                                                             vec)}

                                      :bordure {:type :heraldry.charge-group.type/in-orle
                                                :slots (vec (repeat default-charge-group-amount 0))}

                                      :orle {:type :heraldry.charge-group.type/in-orle
                                             :distance 2.5
                                             :slots (vec (repeat default-charge-group-amount 0))}

                                      {:type :heraldry.charge-group.type/rows
                                       :spacing (/ 95 default-charge-group-amount)
                                       :strips [{:type :heraldry.component/charge-group-strip
                                                 :slots (vec (repeat default-charge-group-amount 0))}]})))

      (= type-namespace
         "heraldry.ordinary.type") (update
                                    :field
                                    (fn [field]
                                      (cond-> field
                                        (:components field) (update
                                                             :components
                                                             (fn [components]
                                                               (mapv
                                                                (fn [component]
                                                                  (add-charge-group-defaults
                                                                   component
                                                                   :parent-ordinary-type type))
                                                                components)))))))))

(defn process-charge-groups [hdn]
  (walk/prewalk add-charge-group-defaults hdn))

(defn replace-adjusted-components [hdn indexed-components]
  (update
   hdn
   :components
   (fn [components]
     (vec
      (loop [components components
             [[index component] & rest] indexed-components]
        (if index
          (recur
           (assoc components index component)
           rest)
          components))))))

(defn arrange-ordinaries-in-one-dimension [hdn indexed-components & {:keys [offset-keyword
                                                                            spacing
                                                                            default-size]}]
  (let [num-elements (count indexed-components)
        size-without-spacing (->> indexed-components
                                  (map second)
                                  (map (fn [component]
                                         (-> component
                                             :geometry
                                             :size
                                             (or default-size))))
                                  (reduce +))
        total-size-with-margin (-> num-elements
                                   inc
                                   (* spacing)
                                   (+ size-without-spacing))
        stretch-factor (min 1
                            (/ 100 total-size-with-margin))
        total-size (-> num-elements
                       dec
                       (* spacing)
                       (+ size-without-spacing)
                       (* stretch-factor))
        adjusted-indexed-components (for [[index [parent-index component]] (map-indexed vector indexed-components)]
                                      (let [size (-> component :geometry :size (or default-size))
                                            size-so-far (->> indexed-components
                                                             (take index)
                                                             (map second)
                                                             (map (fn [component]
                                                                    (-> component
                                                                        :geometry
                                                                        :size
                                                                        (or default-size))))
                                                             (reduce +))
                                            offset (-> size-so-far
                                                       (+ (* index spacing))
                                                       (+ (/ size 2))
                                                       (* stretch-factor)
                                                       (- (/ total-size 2)))]
                                        [parent-index
                                         (-> component
                                             (assoc-in [:geometry :size] (* size stretch-factor))
                                             (assoc-in [:origin offset-keyword] offset))]))]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn arrange-orles [hdn indexed-components]
  (let [indexed-components indexed-components
        default-size 3
        spacing 2
        initial-spacing 3
        adjusted-indexed-components (for [[real-index [parent-index component]] (map-indexed vector indexed-components)]
                                      (let [index (min real-index 5)
                                            size (-> component :geometry :size (or default-size))
                                            size-so-far (->> indexed-components
                                                             (take index)
                                                             (map second)
                                                             (map (fn [component]
                                                                    (-> component
                                                                        :thickness
                                                                        (or default-size))))
                                                             (reduce +))
                                            offset (+ size-so-far
                                                      initial-spacing
                                                      (* index spacing))]
                                        [parent-index
                                         (-> component
                                             (assoc :thickness (if (-> component
                                                                       :line
                                                                       (or :straight)
                                                                       (not= :straight))
                                                                 (/ size 2)
                                                                 size))
                                             (assoc :distance offset))]))]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn arrange-ordinaries [hdn indexed-components]
  (let [ordinary-type (-> indexed-components
                          first
                          second
                          :type
                          name
                          keyword)]
    (case ordinary-type
      :pale (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-x
                                                 :spacing 10
                                                 :default-size 20)
      :pile (arrange-ordinaries-in-one-dimension
             hdn
             (->> indexed-components
                  (map (fn [[index component]]
                         [index (-> component
                                    (assoc-in [:anchor :point] :angle)
                                    (assoc-in [:anchor :angle] 0))])))
             :offset-keyword :offset-x
             :spacing 0
             :default-size 33.333333)
      :fess (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-y
                                                 :spacing 5
                                                 :default-size 15)
      :bend (arrange-ordinaries-in-one-dimension
             hdn
             (->> indexed-components
                  (map (fn [[index component]]
                         [index (-> component
                                    (assoc-in [:anchor :point] :angle)
                                    (assoc-in [:anchor :angle] 45))])))

             :offset-keyword :offset-y
             :spacing 15
             :default-size 15)
      :bend-sinister (arrange-ordinaries-in-one-dimension
                      hdn
                      (->> indexed-components
                           (map (fn [[index component]]
                                  [index (-> component
                                             (assoc-in [:anchor :point] :angle)
                                             (assoc-in [:anchor :angle] 45))])))

                      :offset-keyword :offset-y
                      :spacing 15
                      :default-size 15)
      :chevron (arrange-ordinaries-in-one-dimension
                hdn
                (->> indexed-components
                     (map (fn [[index component]]
                            [index (-> component
                                       (assoc-in [:anchor :point] :angle)
                                       (assoc-in [:anchor :angle] 45)
                                       (assoc-in [:direction-intermediate-name :point] :angle)
                                       (assoc-in [:direction-intermediate-name :angle] 0))])))
                :offset-keyword :offset-y
                :spacing 15
                :default-size 15)
      :orle (arrange-orles hdn indexed-components)
      hdn)))

(defn add-ordinary-defaults [hdn]
  (if (and (map? hdn)
           (some-> hdn :type namespace (= "heraldry.field.type")))
    (let [components-by-type (->> hdn
                                  :components
                                  (map-indexed vector)
                                  (group-by (comp :type second)))]
      (doall
       (loop [hdn hdn
              [[component-type indexed-components] & rest] components-by-type]
         (if component-type
           (recur (if (and (-> indexed-components count (> 1))
                           (-> component-type namespace (= "heraldry.ordinary.type")))
                    (arrange-ordinaries hdn indexed-components)
                    hdn)
                  rest)
           hdn))))
    hdn))

(defn process-ordinary-groups [hdn]
  (walk/postwalk add-ordinary-defaults hdn))

(defn find-best-variant [{:keys [type attitude facing]} charge-map]
  (let [short-charge-type (-> type name keyword)
        candidates (get charge-map short-charge-type)
        candidates-with-attitude (cond->> candidates
                                   attitude (filter (fn [charge]
                                                      (-> charge
                                                          :attitude
                                                          (or :rampant)
                                                          (= attitude)))))
        charge-name (-> short-charge-type
                        name
                        (s/replace "-" " "))
        [attitude-warning
         candidates] (if (and attitude
                              (seq candidates)
                              (empty? candidates-with-attitude))
                       [(str "No charge '"
                             charge-name
                             "' found with attitude '"
                             (-> attitude
                                 name
                                 (s/replace "-" " "))
                             "', using best match.")
                        candidates]
                       [nil
                        candidates-with-attitude])
        candidates-with-facing (cond->> candidates
                                 facing (filter (fn [charge]
                                                  (-> charge
                                                      :facing
                                                      (or :to-dexter)
                                                      (= facing)))))
        [facing-warning
         candidates] (if (and facing
                              (seq candidates)
                              (empty? candidates-with-facing))
                       [(str "No charge '"
                             charge-name
                             "' found facing '"
                             (-> facing
                                 name
                                 (s/replace "-" " "))
                             "', using best match.")
                        candidates]
                       [nil
                        candidates-with-facing])
        warnings (cond-> []
                   attitude-warning (conj attitude-warning)
                   facing-warning (conj facing-warning))]
    (-> candidates
        first
        ;; TODO: this is not ideal
        (or {:id nil
             :version nil})
        (select-keys [:id :version])
        (cond->
          (seq warnings) (assoc ::warnings warnings)))))

(defn is-charge-type? [charge-type]
  (some-> charge-type namespace (= "heraldry.charge.type")))

(defn populate-charge-variants [{:keys [charge-map]} hdn]
  (if (map? hdn)
    (let [charge-type (:type hdn)]
      (if (and (is-charge-type? charge-type)
               (not (get charge.options/choice-map (:type hdn))))
        (let [variant (find-best-variant hdn charge-map)]
          (cond-> hdn
            variant (assoc :variant variant)))
        hdn))
    hdn))

(defn process-charge-references [hdn parser]
  (walk/postwalk (partial populate-charge-variants parser) hdn))

(defn last-tincture [tinctures tincture-same-reference]
  (->> tinctures
       (take-while #(not= % tincture-same-reference))
       (remove map?)
       last))

(defn process-tincture-references [hdn tinctures]
  (let [first-phase (walk/prewalk
                     (fn [data]
                       (if (map? data)
                         (condp apply [data]
                           :heraldry.reader.blazonry.transform/tincture-ordinal-reference
                           :>> (fn [tincture-reference]
                                 (let [index (dec tincture-reference)]
                                   (if (and (<= 0 index)
                                            (< index (count tinctures)))
                                     (get tinctures index)
                                     :void)))

                           :heraldry.reader.blazonry.parser/tincture-same-id
                           (or (last-tincture tinctures data)
                               :void)

                           data)
                         data))
                     hdn)
        root-field-without-components (-> (walk/prewalk
                                           (fn [data]
                                             (if (map? data)
                                               (if (:heraldry.reader.blazonry.transform/tincture-field-reference data)
                                                  ;; if any of the subfields of the root field reference the field,
                                                  ;; then that must be considered :void for this step, so we don't
                                                  ;; get an infinite loop
                                                 :void
                                                 (dissoc data :components))
                                               data))
                                           first-phase)
                                          (select-keys [:type :fields :tincture]))]
    (walk/prewalk
     (fn [data]
       (if (map? data)
         (if (and (-> data :type (= :heraldry.field.type/plain))
                  (-> data :tincture :heraldry.reader.blazonry.transform/tincture-field-reference))
           (-> data
               (dissoc :tincture)
               (merge root-field-without-components))
           (if (:heraldry.reader.blazonry.transform/tincture-field-reference data)
             :void
             data))
         data))
     first-phase)))

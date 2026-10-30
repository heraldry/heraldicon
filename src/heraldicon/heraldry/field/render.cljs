(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.subfield :as subfield]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.render.ir :as ir]))

(defn- subfields-ir [context {:keys [num-subfields transform overlap?-fn]
                              :or {overlap?-fn even?}}]
  ;; TODO: overlap should move into subfield/render-ir
  (let [subfield-irs (mapv (fn [idx]
                             (subfield/render-ir (c/++ context :fields idx) transform (overlap?-fn idx)))
                           (sort-by overlap?-fn > (range num-subfields)))]
    (assoc (reduce ir/combine ir/blank subfield-irs)
           :body (into [:<>] (map :body) subfield-irs))))

(defn- field-path-allowed? [{:keys [path]
                             :as context}]
  (let [component-path (->> path
                            reverse
                            (drop-while (comp not int?)))
        index (first component-path)
        components-path (->> component-path
                             (drop 1)
                             reverse
                             vec)
        counterchanged-paths (c/counterchanged-paths context)
        length (count components-path)]
    (loop [[counterchanged-path & rest] counterchanged-paths]
      (if (nil? counterchanged-path)
        true
        (let [counterchanged-path (vec counterchanged-path)
              start (when (-> counterchanged-path count (>= length))
                      (subvec counterchanged-path 0 length))]
          (if (and (= start components-path)
                   (>= index (get counterchanged-path length)))
            false
            (recur rest)))))))

(defn- components-ir [context]
  (let [component-contexts (into []
                                 (comp (map #(c/++ context :components %))
                                       (take-while field-path-allowed?))
                                 (range (interface/get-list-size (c/++ context :components))))]
    (-> (reduce ir/add-child ir/blank component-contexts)
        (assoc :body (into [:<>]
                           (map (fn [component-context]
                                  [interface/ir-node component-context]))
                           component-contexts)))))

(defn- counterchanged-field-ir [{:keys [path]
                                 :as context}]
  (if-let [parent-field-context (interface/parent (interface/parent context))]
    (let [counterchange-tinctures (interface/counterchanged-tinctures parent-field-context)
          counterchanged-context (-> parent-field-context
                                     (c/add-counterchanged-path path)
                                     (c/add-counterchanged-tinctures counterchange-tinctures))]
      (-> ir/blank
          (ir/add-child counterchanged-context)
          (assoc :body [interface/ir-node counterchanged-context])))
    ir/blank))

(defmethod interface/render-ir :heraldry/field [context]
  (let [{:keys [render-fn]
         field-type :type
         :as properties} (interface/get-properties context)
        field-ir (case field-type
                   :heraldry.field.type/plain (assoc ir/blank :body [tincture/tinctured-field context])
                   :heraldry.field.type/counterchanged (counterchanged-field-ir context)
                   (if render-fn
                     (assoc ir/blank :body [render-fn context properties])
                     (subfields-ir context properties)))
        components (components-ir context)]
    (-> (ir/combine field-ir components)
        (assoc :body [:<>
                      (:body field-ir)
                      [render/field-edges context]
                      (:body components)]))))

(defmethod interface/render-component :heraldry/field [context]
  [interface/render-ir-root context])

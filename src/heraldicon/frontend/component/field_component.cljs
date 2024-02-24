(ns heraldicon.frontend.component.field-component
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.element :as component.element]
   [re-frame.core :as rf]))

(defn inside-own-subtree?
  [dragged-node-path drop-node-path]
  (= (take (count dragged-node-path) drop-node-path)
     dragged-node-path))

(defn component?
  [dragged-node-path]
  (-> dragged-node-path
      drop-last
      last
      (= :components)))

(defn parent?
  [dragged-node-path drop-node-path]
  (= (drop-last 3 dragged-node-path)
     drop-node-path))

(defn sibling?
  [dragged-node-path drop-node-path]
  (= (drop-last dragged-node-path)
     (drop-last drop-node-path)))

(defn drop-options-fn
  [dragged-node-path dragged-node-type
   drop-node-path drop-node-type
   drop-node-open?]
  (let [sibling? (sibling?
                  dragged-node-path
                  drop-node-path)]
    (cond-> (cond
              (inside-own-subtree?
               dragged-node-path
               drop-node-path) nil

              (not (component?
                    dragged-node-path)) nil

              (and (isa? dragged-node-type
                         :heraldry/shield-separator)
                   (not sibling?)) nil

              (parent?
               dragged-node-path
               drop-node-path) #{:above :below}

              sibling? (let [current-index (last dragged-node-path)
                             new-index (last drop-node-path)]
                         (cond
                           (= new-index
                              (dec current-index)) #{:above :inside}

                           (= new-index
                              (inc current-index)) #{:below :inside}

                           :else #{:above :inside :below}))

              :else #{:above :inside :below})
      drop-node-open? (disj :below)

      (isa? drop-node-type
            :heraldry/charge-group) (disj :inside)

      (isa? drop-node-type
            :heraldry/semy) (disj :inside)

      (isa? dragged-node-type
            :heraldry/shield-separator) (disj :inside))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [new-index (last (:path drop-node-context))
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :inside (c/++ drop-node-context :field :components component.element/APPEND-INDEX)
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (rf/dispatch [::component.element/move-general
                  dragged-node-context
                  target-context])))

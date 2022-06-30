(ns heraldicon.frontend.js-event)

(defn stop-propagation
  ([] (stop-propagation nil))
  ([f] (fn [event]
         (.stopPropagation event)
         (when f
           (f event)))))

(defn prevent-default
  ([] (prevent-default nil))
  ([f] (fn [event]
         (.preventDefault event)
         (when f
           (f event)))))

(defn handled
  ([] (handled nil))
  ([f] (-> f
           stop-propagation
           prevent-default)))

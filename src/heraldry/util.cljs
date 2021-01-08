(ns heraldry.util)

(defn promise
  [resolver]
  (js/Promise. resolver))

(defn promise-from-callback
  [f]
  (promise (fn [resolve reject]
             (f (fn [error data]
                  (if (nil? error)
                    (resolve data)
                    (reject error)))))))

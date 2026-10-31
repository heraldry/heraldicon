(ns heraldicon.interface.macros
  (:require
   [clojure.string :as str]))

(defmacro defattribute
  "Defines a cached attribute of a component: a subscription that computes body
  once per context, and the function name that reads it. In a server render
  the function computes body directly, memoized for the render, see
  heraldicon.interface/*static-cache*.

  The last argument is the context, any arguments before it are part of the
  cache key as well. The function scrubs the render hints from the context, so
  rendering hints don't split the cache. The subscription id is name without
  its get- prefix, in heraldicon.interface.

    (defattribute get-bounding-box [context]
      (bounding-box (resolve-context context)))"
  [name & fdecl]
  (let [[doc [args & body]] (if (string? (first fdecl))
                              [(first fdecl) (rest fdecl)]
                              [nil fdecl])
        sub-id (keyword "heraldicon.interface" (str/replace (str name) #"^get-" ""))
        extra-args (vec (butlast args))
        context (last args)]
    (assert (and (symbol? context)
                 (every? symbol? extra-args))
            "defattribute arguments must be plain symbols")
    `(do
       ;; attributes can be recursive, e.g. a component's options depend on its parent's
       (declare ~name)

       (re-frame.core/reg-sub-raw
         ~sub-id
         (fn [_# [_# ~@args]]
           (reagent.ratom/make-reaction (fn [] ~@body))))

       (defn ~name ~@(when doc [doc]) ~args
         (let [~context (heraldicon.context/scrub-render-hints ~context)]
           (if heraldicon.interface/*static-cache*
             (heraldicon.interface/static-value [~sub-id ~@extra-args ~context] (fn [] ~@body))
             @(re-frame.core/subscribe [~sub-id ~@extra-args ~context])))))))

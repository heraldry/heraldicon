(ns heraldicon.heraldry.cottising
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]))

(defn- add-cottise-options [options key context & {:keys [size-reference-default]}]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside)
                       (options/update-if-exists [:height :default] #(/ % 3))
                       (cond->
                         size-reference-default (options/override-if-exists [:size-reference :default] size-reference-default)))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (options/update-if-exists [:height :default] #(/ % 3))
                                (cond->
                                  size-reference-default (options/override-if-exists [:size-reference :default] size-reference-default)))]
    (assoc options
           key
           {:line (assoc line-style :ui/label :string.entity/line)
            :opposite-line (assoc opposite-line-style :ui/label :string.entity/opposite-line)
            :distance {:type :option.type/range
                       :min -10
                       :max 20
                       :default 2
                       :ui/label :string.option/distance
                       :ui/step 0.1}
            :thickness {:type :option.type/range
                        :min 0.1
                        :max 20
                        :default 2
                        :ui/label :string.option/thickness
                        :ui/step 0.1}
            :outline? {:type :option.type/boolean
                       :default false
                       :ui/label :string.charge.tincture-modifier.special/outline}
            :ui/element :ui.element/cottising})))

(defn add-cottising [context num & {:as opts}]
  (let [cottising-context (c/++ context :cottising)]
    (cond-> {}
      (>= num 1) (->
                   (add-cottise-options :cottise-1 (c/++ cottising-context :cottise-1) opts)
                   (add-cottise-options :cottise-2 (c/++ cottising-context :cottise-2) opts))
      (>= num 2) (->
                   (add-cottise-options :cottise-opposite-1 (c/++ cottising-context :cottise-opposite-1) opts)
                   (add-cottise-options :cottise-opposite-2 (c/++ cottising-context :cottise-opposite-2) opts))
      (>= num 3) (->
                   (add-cottise-options :cottise-extra-1 (c/++ cottising-context :cottise-extra-1) opts)
                   (add-cottise-options :cottise-extra-2 (c/++ cottising-context :cottise-extra-2) opts)))))

; :heraldry/cottise is a special case, it's a UI component, but not a :heraldry.options/root,
; because it is a child of the parent :cottising

(defmethod interface/options :heraldry/cottise [_context]
  nil)

(defn kind [context]
  (-> context :path last))

(defmulti cottise-properties (fn [_context reference-properties]
                               (:type reference-properties)))

(defmethod cottise-properties nil [_context _properties])

(defmethod interface/properties :heraldry/cottise [context]
  (let [reference-context (case (kind context)
                            :cottise-2 (-> context c/-- (c/++ :cottise-1))
                            :cottise-opposite-2 (-> context c/-- (c/++ :cottise-opposite-1))
                            :cottise-extra-2 (-> context c/-- (c/++ :cottise-extra-1))
                            (c/-- context 2))
        reference-properties (interface/get-properties reference-context)]
    (cottise-properties context reference-properties)))

(defn cottise-height [context line-length percentage-base]
  (if (interface/get-raw-data (c/++ context :type))
    (let [distance (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :distance)))
          thickness (math/percent-of percentage-base
                                     (interface/get-sanitized-data (c/++ context :thickness)))
          line-properties (post-process/line-properties {:line-length line-length} context)
          kind! (kind context)]
      (+ distance
         thickness
         (cond
           (#{:cottise-1
              :cottise-2} kind!) (-> line-properties :line :effective-height)
           (#{:opposite-cottise-1
              :opposite-cottise-2} kind!) (-> line-properties :opposite-line :effective-height)
           :else 0)))
    0))

(defmethod interface/environment :heraldry/cottise [context]
  (let [reference-type (:type (interface/get-properties context))]
    ((get-method interface/environment reference-type) context)))

(defmethod interface/render-shape :heraldry/cottise [context]
  (let [reference-type (:type (interface/get-properties context))]
    ((get-method interface/render-shape reference-type) context)))

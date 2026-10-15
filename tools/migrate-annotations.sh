#!/usr/bin/env bash
# Migrate the Java sources of an archidata application from the JPA annotations
# (jakarta.persistence) and the JAX-RS @DefaultValue on entity fields to the archidata
# annotations (@Collection, @Field, @Id, @InitValue). See MIGRATION.md.
#
# Usage: tools/migrate-annotations.sh <source folder> [<source folder>...]
#
# The files are modified in place: run it on a clean git working tree and review the result
# with `git diff`. Mechanical replacements only; everything else is listed at the end, to be
# handled by hand. Needs GNU sed.
set -euo pipefail

if [ "$#" -eq 0 ]; then
	echo "Usage: $0 <source folder> [<source folder>...]" >&2
	exit 1
fi
if ! sed --version >/dev/null 2>&1; then
	echo "GNU sed is required (on macOS: brew install gnu-sed, then put gsed first in the PATH)" >&2
	exit 1
fi
for folder in "$@"; do
	if [ ! -d "$folder" ]; then
		echo "Not a folder: $folder" >&2
		exit 1
	fi
done

readonly PKG="org.atriasoft.archidata.annotation"
# JAX-RS parameter annotations: a file that has one is a REST resource, whose @DefaultValue
# belongs to Jersey and must stay.
readonly JAXRS_PARAMS='@(Query|Path|Header|Form|Matrix|Cookie|Bean)Param\b'

changed=0
fqn_files=()
jaxrs_files=()

# replace_import <file> <old import> <old annotation regex> <new import> <new annotation regex>
# Swap the import when the old annotation is no longer used in the file; keep the old import
# (and add the new one) when some usages are left for a manual migration.
replace_import() {
	local file="$1" old_import="$2" old_usage="$3" new_import="$4" new_usage="$5"
	grep -q "^import ${old_import//./\\.};" "$file" || return 0
	local add_new=0
	if [ -n "$new_import" ] && grep -qE "$new_usage" "$file"; then
		add_new=1
	fi
	# Usages left in the code (comments excluded: a mention in a comment needs no import)
	if grep -vE '^\s*(\*|//|/\*)' "$file" | grep -qE "$old_usage"; then
		if [ "$add_new" -eq 1 ]; then
			sed -i "s/^import ${old_import//./\\.};\$/&\nimport ${new_import};/" "$file"
		fi
	elif [ "$add_new" -eq 1 ]; then
		sed -i "s/^import ${old_import//./\\.};\$/import ${new_import};/" "$file"
	else
		sed -i "/^import ${old_import//./\\.};\$/d" "$file"
	fi
}

while IFS= read -r -d '' file; do
	grep -qE 'jakarta\.persistence|jakarta\.ws\.rs\.DefaultValue' "$file" || continue
	before=$(md5sum <"$file")

	# Name clash with java.lang.reflect.Field / java.util.Collection: write the archidata
	# annotation with its full name in that file instead of importing it.
	field_ann="@Field"
	field_import="$PKG.Field"
	if grep -qE '^import java\.lang\.reflect\.Field;' "$file"; then
		field_ann="@$PKG.Field"
		field_import=""
	fi
	collection_ann="@Collection"
	collection_import="$PKG.Collection"
	if grep -qE '^import java\.util\.Collection;' "$file"; then
		collection_ann="@$PKG.Collection"
		collection_import=""
	fi

	# @Table(name = "x") / @Table(name = CONSTANT) -> @Collection("x")
	sed -i -E "s/@Table\(\s*name\s*=\s*(\"[^\"]*\"|[A-Za-z_][A-Za-z0-9_.]*)\s*\)/${collection_ann}(\1)/g" "$file"
	replace_import "$file" jakarta.persistence.Table '@Table\b' "$collection_import" '@Collection\('

	# @Column(name = "x") -> @Field("x"); a bare @Column (no attribute) is dropped
	sed -i -E "s/@Column\(\s*name\s*=\s*(\"[^\"]*\"|[A-Za-z_][A-Za-z0-9_.]*)\s*\)/${field_ann}(\1)/g" "$file"
	sed -i -E '/^\s*@Column(\(\s*\))?\s*$/d' "$file"
	sed -i -E 's/@Column(\(\s*\))?\s+([^(])/\2/g' "$file"
	replace_import "$file" jakarta.persistence.Column '@Column\b' "$field_import" '@Field\('

	# @Id: same name, other package
	sed -i -E "s/^import jakarta\.persistence\.Id;\$/import $PKG.Id;/" "$file"

	# @Entity (without attribute) was never read: dropped
	sed -i -E '/^\s*@Entity(\(\s*\))?\s*$/d' "$file"
	replace_import "$file" jakarta.persistence.Entity '@Entity\b' "" ''

	# @GeneratedValue had no effect: dropped
	sed -i -E '/^\s*@GeneratedValue(\([^)]*\))?\s*$/d' "$file"
	sed -i -E 's/@GeneratedValue(\([^)]*\))?\s+//g' "$file"
	replace_import "$file" jakarta.persistence.GeneratedValue '@GeneratedValue\b' "" ''
	replace_import "$file" jakarta.persistence.GenerationType 'GenerationType\.' "" ''

	# @DefaultValue on entity fields -> @InitValue; untouched in the REST resources
	if grep -q '^import jakarta\.ws\.rs\.DefaultValue;' "$file"; then
		if grep -qE "$JAXRS_PARAMS" "$file"; then
			jaxrs_files+=("$file")
		else
			sed -i -E 's/@DefaultValue\(/@InitValue(/g' "$file"
			replace_import "$file" jakarta.ws.rs.DefaultValue '@DefaultValue\b' "$PKG.InitValue" '@InitValue\('
		fi
	fi

	if [ "$(md5sum <"$file")" != "$before" ]; then
		changed=$((changed + 1))
		if grep -qE "@${PKG//./\\.}\.(Field|Collection)\b" "$file"; then
			fqn_files+=("$file")
		fi
	fi
done < <(find "$@" -type f -name '*.java' -print0 | sort -z)

echo "== $changed file(s) modified. Review with 'git diff', then organize the imports (IDE) and run the formatter."
echo

# report <title> <regex> <folder>...
report() {
	local title="$1" regex="$2"
	shift 2
	local hits
	hits=$(grep -rnE --include='*.java' "$regex" "$@" 2>/dev/null \
		| grep -vE 'import jakarta\.persistence\.(Column|Table|ManyToOne|OneToMany|ManyToMany|OneToOne|JoinColumn|JoinTable|FetchType|CascadeType);' \
		|| true)
	if [ -n "$hits" ]; then
		echo "-- $title"
		echo "$hits"
		echo
	fi
}

report "@Column with attributes other than name (nullable/length/unique/insertable/updatable had no effect: delete them, or use @NotNull, @Size(max = ...), @Indexed(unique = true); keep the name in @Field(\"...\"))" \
	'@Column\b' "$@"
report "@Table with attributes other than name: write @Collection(\"...\")" '@Table\b' "$@"
report "JPA relations: replace with @ManyToOneDoc / @OneToManyDoc / @ManyToManyDoc (targetEntity, remoteField)" \
	'@(ManyToOne|OneToMany|ManyToMany|OneToOne|JoinColumn|JoinTable)\b' "$@"
report "Other jakarta.persistence usages (not read by archidata: delete them, or find an archidata equivalent)" \
	'jakarta\.persistence' "$@"

if [ "${#jaxrs_files[@]}" -gt 0 ]; then
	echo "-- @DefaultValue left untouched in REST resources (JAX-RS parameters, handled by Jersey):"
	echo "   check that none of them is on an entity field (it would need @InitValue)"
	printf '   %s\n' "${jaxrs_files[@]}"
	echo
fi
if [ "${#fqn_files[@]}" -gt 0 ]; then
	echo "-- Full name used for @Field/@Collection (the file imports java.lang.reflect.Field or java.util.Collection):"
	printf '   %s\n' "${fqn_files[@]}"
	echo
fi

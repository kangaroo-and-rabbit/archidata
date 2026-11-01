package test.atriasoft.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModelList;
import org.atriasoft.archidata.externalRestApi.typescript.ImportModel.ModeImport;
import org.atriasoft.archidata.externalRestApi.typescript.ImportModel.PairElem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The descriptors shown to the generators keep their arrays and lists. */
class TestModelArrays {

	public record Sample(
			String name,
			Long size) {}

	@Test
	void parameterClassModelKeepsItsGroups() {
		final Class<?>[] given = { GroupCreate.class };
		final ParameterClassModel model = new ParameterClassModel(true, given, null);
		given[0] = GroupUpdate.class;
		model.groups()[0] = GroupUpdate.class;
		Assertions.assertArrayEquals(new Class<?>[] { GroupCreate.class }, model.groups());
		Assertions.assertArrayEquals(new Class<?>[] { GroupRead.class },
				new ParameterClassModel(true, null, null).groups());
	}

	@Test
	void parameterClassModelListKeepsItsGroupsAndModels() {
		final Class<?>[] given = { GroupCreate.class };
		final List<org.atriasoft.archidata.externalRestApi.model.ClassModel> models = new ArrayList<>();
		final ParameterClassModelList list = new ParameterClassModelList(true, given, models, false);
		given[0] = GroupUpdate.class;
		list.groups()[0] = GroupUpdate.class;
		models.add(null);
		Assertions.assertArrayEquals(new Class<?>[] { GroupCreate.class }, list.groups());
		Assertions.assertTrue(list.models().isEmpty());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> list.models().add(null));
		Assertions.assertNull(new ParameterClassModelList(true, (Class<?>[]) null, models, false).groups());
	}

	@Test
	void importPairKeepsItsGroups() {
		final Class<?>[] given = { GroupCreate.class };
		final PairElem pair = new PairElem(ModeImport.ZOD, true, given);
		given[0] = GroupUpdate.class;
		pair.groups()[0] = GroupUpdate.class;
		Assertions.assertArrayEquals(new Class<?>[] { GroupCreate.class }, pair.groups());
		Assertions.assertEquals(new PairElem(ModeImport.ZOD, true, new Class<?>[] { GroupCreate.class }), pair);
	}

	@Test
	void aConstructorDescriptorKeepsItsParameters() throws Exception {
		final ClassModel model = ClassModel.of(Sample.class);
		final String[] names = model.getConstructors().get(0).parameterNames();
		names[0] = "changed";
		Assertions.assertEquals("name", model.getConstructors().get(0).parameterNames()[0]);
		Assertions.assertEquals(new Sample("a", 1L), model.newInstance(Map.of("name", "a", "size", 1L)));
	}
}
